package com.ryanharter.auto.value.parcel;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.SimpleTypeVisitor7;
import javax.lang.model.util.Types;

final class Parcelables {

  static final TypeName STRING = ClassName.get("java.lang", "String");
  static final TypeName MAP = ClassName.get("java.util", "Map");
  static final TypeName LIST = ClassName.get("java.util", "List");
  static final TypeName BOOLEANARRAY = ArrayTypeName.of(boolean.class);
  static final TypeName BYTEARRAY = ArrayTypeName.of(byte.class);
  static final TypeName CHARARRAY = ArrayTypeName.of(char.class);
  static final TypeName INTARRAY = ArrayTypeName.of(int.class);
  static final TypeName LONGARRAY = ArrayTypeName.of(long.class);
  static final TypeName STRINGARRAY = ArrayTypeName.of(String.class);
  static final TypeName SPARSEARRAY = ClassName.get("android.util", "SparseArray");
  static final TypeName SPARSEBOOLEANARRAY = ClassName.get("android.util", "SparseBooleanArray");
  static final TypeName BUNDLE = ClassName.get("android.os", "Bundle");
  static final TypeName PARCELABLE = ClassName.get("android.os", "Parcelable");
  static final TypeName PARCELABLEARRAY = ArrayTypeName.of(PARCELABLE);
  static final TypeName CHARSEQUENCE = ClassName.get("java.lang", "CharSequence");
  static final TypeName IBINDER = ClassName.get("android.os", "IBinder");
  static final TypeName OBJECTARRAY = ArrayTypeName.of(TypeName.OBJECT);
  static final TypeName SERIALIZABLE = ClassName.get("java.io", "Serializable");
  static final TypeName PERSISTABLEBUNDLE = ClassName.get("android.os", "PersistableBundle");
  static final TypeName SIZE = ClassName.get("android.util", "Size");
  static final TypeName SIZEF = ClassName.get("android.util", "SizeF");
  static final TypeName TEXTUTILS = ClassName.get("android.text", "TextUtils");
  static final TypeName ENUM = ClassName.get(Enum.class);

  private static final Set<TypeName> VALID_TYPES = ImmutableSet.of(STRING, MAP, LIST, BOOLEANARRAY,
      BYTEARRAY, CHARARRAY, INTARRAY, LONGARRAY, STRINGARRAY, SPARSEARRAY, SPARSEBOOLEANARRAY,
      BUNDLE, PARCELABLE, PARCELABLEARRAY, CHARSEQUENCE, IBINDER, OBJECTARRAY,
      SERIALIZABLE, PERSISTABLEBUNDLE, SIZE, SIZEF);

  public static boolean isValidType(TypeName typeName) {
    return typeName.isPrimitive() || typeName.isBoxedPrimitive() || VALID_TYPES.contains(typeName);
  }

  public static boolean isValidType(Types types, TypeMirror type) {
    // Special case for MAP, since it can only have String keys and Parcelable values
    if (isOfType(types, type, MAP)) {
      return isValidMap(types, type);
    }

    return getParcelableType(types, (TypeElement) types.asElement(type)) != null;
  }

  public static TypeName getParcelableType(Types types, TypeElement type) {
    TypeMirror typeMirror = type.asType();
    while (typeMirror.getKind() != TypeKind.NONE) {
      TypeName typeName = TypeName.get(typeMirror);

      // first, check if the class is valid.
      if (typeName instanceof ParameterizedTypeName) {
        typeName = ((ParameterizedTypeName) typeName).rawType;
      }
      if (isValidType(typeName)) {
        return typeName;
      }

      // then check if it implements valid interfaces
      for (TypeMirror iface : type.getInterfaces()) {
        TypeName inherited = getParcelableType(types, (TypeElement) types.asElement(iface));
        if (inherited != null) {
          return inherited;
        }
      }

      // then move on
      type = (TypeElement) types.asElement(typeMirror);
      typeMirror = type.getSuperclass();
    }
    return null;
  }

  /**
   * Maps can only have String keys and Parcelable values.
   */
  private static boolean isValidMap(Types types, TypeMirror type) {
    return type.accept(new SimpleTypeVisitor6<Boolean, Types>() {
      @Override public Boolean visitDeclared(DeclaredType t, Types o) {
        List<? extends TypeMirror> args = t.getTypeArguments();
        if (args.size() == 2) {
          TypeMirror key = args.get(0);
          TypeMirror value = args.get(1);
          if (STRING.equals(TypeName.get(key)) && isValidType(o, value)) {
            return true;
          }
        }
        return false;
      }
    }, types);
  }

  private static boolean isOfType(Types types, TypeMirror typeMirror, TypeName target) {
    TypeElement element = (TypeElement) types.asElement(typeMirror);
    while (typeMirror.getKind() != TypeKind.NONE) {
      TypeName typeName = TypeName.get(typeMirror);
      if (typeName instanceof ParameterizedTypeName) {
        typeName = ((ParameterizedTypeName) typeName).rawType;
      }
      if (typeName.equals(target)) {
        return true;
      }

      for (TypeMirror iface : element.getInterfaces()) {
        if (isOfType(types, iface, target)) {
          return true;
        }
      }

      element = (TypeElement) types.asElement(typeMirror);
      typeMirror = element.getSuperclass();
    }
    return false;
  }

  /**
   * Returns the component of the type that is Parcelable, or descends from a Parcelable type.
   */
  private static TypeName getParcelableComponent(final Types types, TypeMirror type) {
    final TypeMirror typeFinal;
    if (type.getKind() == TypeKind.TYPEVAR) {
      TypeVariable vType = (TypeVariable) type;
      typeFinal = vType.getUpperBound();
    } else {
      typeFinal = type;
    }
    return typeFinal.accept(new SimpleTypeVisitor7<TypeName, Void>() {
      @Override
      public TypeName visitDeclared(DeclaredType t, Void aVoid) {
        TypeName typeFinal = TypeName.get(t);
        while (typeFinal instanceof ParameterizedTypeName && !((ParameterizedTypeName) typeFinal).typeArguments.isEmpty()) {
          List<TypeName> args = ((ParameterizedTypeName) typeFinal).typeArguments;
          typeFinal = args.get(args.size() - 1);
        }
        return typeFinal;
      }

      @Override public TypeName visitArray(ArrayType t, Void aVoid) {
        TypeMirror component = t.getComponentType();
        if (getParcelableType(types, (TypeElement) types.asElement(component)) != null) {
          return TypeName.get(component);
        }

        return TypeName.get(t);
      }
    }, null);
  }

  static void readValue(CodeBlock.Builder block, AutoValueParcelExtension.Property property,
      final TypeName parcelableType, Types types) {
    boolean needsNullCheck = needsNullCheck(property, parcelableType);
    if (needsNullCheck){
      block.add("in.readInt() == 0 ? ");
    }

    if (parcelableType.equals(STRING)) {
      block.add("in.readString()");
    } else if (parcelableType.equals(TypeName.BYTE) || parcelableType.equals(TypeName.BYTE.box())) {
      block.add("in.readByte()");
    } else if (parcelableType.equals(TypeName.INT) || parcelableType.equals(TypeName.INT.box())) {
      block.add("in.readInt()");
    } else if (parcelableType.equals(TypeName.SHORT) || parcelableType.equals(TypeName.SHORT.box())) {
      block.add("(short) in.readInt()");
    } else if (parcelableType.equals(TypeName.CHAR) || parcelableType.equals(TypeName.CHAR.box())) {
      block.add("(char) in.readInt()");
    } else if (parcelableType.equals(TypeName.LONG) || parcelableType.equals(TypeName.LONG.box())) {
      block.add("in.readLong()");
    } else if (parcelableType.equals(TypeName.FLOAT) || parcelableType.equals(TypeName.FLOAT.box())) {
      block.add("in.readFloat()");
    } else if (parcelableType.equals(TypeName.DOUBLE) || parcelableType.equals(TypeName.DOUBLE.box())) {
      block.add("in.readDouble()");
    } else if (parcelableType.equals(TypeName.BOOLEAN) || parcelableType.equals(TypeName.BOOLEAN.box())) {
      block.add("in.readInt() == 1");
    } else if (parcelableType.equals(PARCELABLE)) {
      TypeName check = property.type instanceof TypeVariableName
              ? ((TypeVariableName) property.type).bounds.get(0)
              : property.type;
      if (check.equals(PARCELABLE)) {
        block.add("in.readParcelable($T.class.getClassLoader())",
            getParcelableComponent(types, property.element.getReturnType()));
      } else {
        block.add("($T) in.readParcelable($T.class.getClassLoader())", property.type,
            getParcelableComponent(types, property.element.getReturnType()));
      }
    } else if (parcelableType.equals(CHARSEQUENCE)) {
      block.add("$T.CHAR_SEQUENCE_CREATOR.createFromParcel(in)", TEXTUTILS);
    } else if (parcelableType.equals(MAP)) {
      block.add("($T) in.readHashMap($T.class.getClassLoader())", property.type,
          getParcelableComponent(types, property.element.getReturnType()));
    } else if (parcelableType.equals(LIST)) {
      block.add("($T) in.readArrayList($T.class.getClassLoader())", property.type,
          getParcelableComponent(types, property.element.getReturnType()));
    } else if (parcelableType.equals(BOOLEANARRAY)) {
      block.add("in.createBooleanArray()");
    } else if (parcelableType.equals(BYTEARRAY)) {
      block.add("in.createByteArray()");
    } else if (parcelableType.equals(CHARARRAY)) {
      block.add("in.createCharArray()");
    } else if (parcelableType.equals(STRINGARRAY)) {
      block.add("in.readStringArray()");
    } else if (parcelableType.equals(IBINDER)) {
      if (property.type.equals(IBINDER)) {
        block.add("in.readStrongBinder()");
      } else {
        block.add("($T) in.readStrongBinder()", property.type);
      }
    } else if (parcelableType.equals(OBJECTARRAY)) {
      block.add("in.readArray($T.class.getClassLoader())",
          getParcelableComponent(types, property.element.getReturnType()));
    } else if (parcelableType.equals(INTARRAY)) {
      block.add("in.createIntArray()");
    } else if (parcelableType.equals(LONGARRAY)) {
      block.add("in.createLongArray()");
    } else if (parcelableType.equals(SERIALIZABLE)) {
      if (property.type.equals(SERIALIZABLE)) {
        block.add("in.readSerializable()");
      } else {
        block.add("($T) in.readSerializable()", property.type);
      }
    } else if (parcelableType.equals(PARCELABLEARRAY)) {
        ArrayTypeName atype = (ArrayTypeName) property.type;
        if (atype.componentType.equals(PARCELABLE)) {
          block.add("in.readParcelableArray($T.class.getClassLoader())",
              getParcelableComponent(types, property.element.getReturnType()));
        } else {
          block.add("($T) in.readParcelableArray($T.class.getClassLoader())", property.type,
              getParcelableComponent(types, property.element.getReturnType()));
        }
    } else if (parcelableType.equals(SPARSEARRAY)) {
      block.add("in.readSparseArray($T.class.getClassLoader())",
          getParcelableComponent(types, property.element.getReturnType()));
    } else if (parcelableType.equals(SPARSEBOOLEANARRAY)) {
      block.add("in.readSparseBooleanArray()");
    } else if (parcelableType.equals(BUNDLE)) {
      block.add("in.readBundle($T.class.getClassLoader())", property.type);
    } else if (parcelableType.equals(PERSISTABLEBUNDLE)) {
      block.add("in.readPersistableBundle($T.class.getClassLoader())", property.type);
    } else if (parcelableType.equals(SIZE)) {
      block.add("in.readSize()");
    } else if (parcelableType.equals(SIZEF)) {
      block.add("in.readSizeF()");
    } else if (parcelableType.equals(ENUM)) {
      block.add("$T.valueOf(in.readString())", property.type);
    } else {
      block.add("($T) in.readValue($T.class.getClassLoader())", property.type,
          getParcelableComponent(types, property.element.getReturnType()));
    }

    if (needsNullCheck){
      block.add(" : null");
    }
  }

  static void readValueWithTypeAdapter(CodeBlock.Builder block, AutoValueParcelExtension.Property property, final FieldSpec adapter) {
    if (property.nullable()){
      block.add("in.readInt() == 0 ? ");
    }

    block.add("$N.fromParcel(in)", adapter);

    if (property.nullable()){
      block.add(" : null");
    }
  }

  public static CodeBlock writeValue(Types types, AutoValueParcelExtension.Property property,
      ParameterSpec out, ParameterSpec flags) {
    CodeBlock.Builder block = CodeBlock.builder();

    TypeName type = getTypeNameFromProperty(property, types);

    boolean needsNullCheck = needsNullCheck(property, type);
    if (needsNullCheck) {
      block.beginControlFlow("if ($N() == null)", property.methodName);
      block.addStatement("$N.writeInt(1)", out);
      block.nextControlFlow("else");
      block.addStatement("$N.writeInt(0)", out);
    }

    if (type.equals(STRING))
      block.add("$N.writeString($N())", out, property.methodName);
    else if (type.equals(TypeName.BYTE) || type.equals(TypeName.BYTE.box())
        || type.equals(TypeName.INT) || type.equals(TypeName.INT.box())
        || type.equals(TypeName.CHAR) || type.equals(TypeName.CHAR.box())
        || type.equals(TypeName.SHORT))
      block.add("$N.writeInt($N())", out, property.methodName);
    else if (type.equals(TypeName.SHORT.box()))
      block.add("$N.writeInt($N().intValue())", out, property.methodName);
    else if (type.equals(TypeName.LONG) || type.equals(TypeName.LONG.box()))
      block.add("$N.writeLong($N())", out, property.methodName);
    else if (type.equals(TypeName.FLOAT) || type.equals(TypeName.FLOAT.box()))
      block.add("$N.writeFloat($N())", out, property.methodName);
    else if (type.equals(TypeName.DOUBLE) || type.equals(TypeName.DOUBLE.box()))
      block.add("$N.writeDouble($N())", out, property.methodName);
    else if (type.equals(TypeName.BOOLEAN) || type.equals(TypeName.BOOLEAN.box()))
      block.add("$N.writeInt($N() ? 1 : 0)", out, property.methodName);
    else if (type.equals(PARCELABLE))
      block.add("$N.writeParcelable($N(), $N)", out, property.methodName, flags);
    else if (type.equals(CHARSEQUENCE))
      block.add("$T.writeToParcel($N(), $N, $N)", TEXTUTILS, property.methodName, out, flags);
    else if (type.equals(MAP))
      block.add("$N.writeMap($N())", out, property.methodName);
    else if (type.equals(LIST))
      block.add("$N.writeList($N())", out, property.methodName);
    else if (type.equals(BOOLEANARRAY))
      block.add("$N.writeBooleanArray($N())", out, property.methodName);
    else if (type.equals(BYTEARRAY))
      block.add("$N.writeByteArray($N())", out, property.methodName);
    else if (type.equals(CHARARRAY))
      block.add("$N.writeCharArray($N())", out, property.methodName);
    else if (type.equals(STRINGARRAY))
      block.add("$N.writeStringArray($N())", out, property.methodName);
    else if (type.equals(IBINDER))
      block.add("$N.writeStrongBinder($N())", out, property.methodName);
    else if (type.equals(OBJECTARRAY))
      block.add("$N.writeArray($N())", out, property.methodName);
    else if (type.equals(INTARRAY))
      block.add("$N.writeIntArray($N())", out, property.methodName);
    else if (type.equals(LONGARRAY))
      block.add("$N.writeLongArray($N())", out, property.methodName);
    else if (type.equals(SERIALIZABLE))
      block.add("$N.writeSerializable($N())", out, property.methodName);
    else if (type.equals(PARCELABLEARRAY))
      block.add("$N.writeParcelableArray($N())", out, property.methodName);
    else if (type.equals(SPARSEARRAY))
      block.add("$N.writeSparseArray($N())", out, property.methodName);
    else if (type.equals(SPARSEBOOLEANARRAY))
      block.add("$N.writeSparseBooleanArray($N())", out, property.methodName);
    else if (type.equals(BUNDLE))
      block.add("$N.writeBundle($N())", out, property.methodName);
    else if (type.equals(PERSISTABLEBUNDLE))
      block.add("$N.writePersistableBundle($N())", out, property.methodName);
    else if (type.equals(SIZE))
      block.add("$N.writeSize($N())", out, property.methodName);
    else if (type.equals(SIZEF))
      block.add("$N.writeSizeF($N())", out, property.methodName);
    else if (type.equals(ENUM))
      block.add("$N.writeString($N().name())", out, property.methodName);
    else
      block.add("$N.writeValue($N())", out, property.methodName);

    block.add(";\n");

    if (needsNullCheck) {
      block.endControlFlow();
    }

    return block.build();
  }

  private static boolean needsNullCheck(AutoValueParcelExtension.Property property, TypeName type) {
    return property.nullable()
        && !type.equals(BUNDLE)
        && !type.equals(LIST)
        && !type.equals(MAP)
        && !type.equals(PARCELABLE)
        && !type.equals(PERSISTABLEBUNDLE)
        && !type.equals(SPARSEARRAY)
        && !type.equals(SPARSEBOOLEANARRAY);
  }

  public static CodeBlock writeValueWithTypeAdapter(FieldSpec adapter, AutoValueParcelExtension.Property property, ParameterSpec out) {
    CodeBlock.Builder block = CodeBlock.builder();

    if (property.nullable()) {
      block.beginControlFlow("if ($N() == null)", property.methodName);
      block.addStatement("$N.writeInt(1)", out);
      block.nextControlFlow("else");
      block.addStatement("$N.writeInt(0)", out);
    }

    block.addStatement("$N.toParcel($N(), $N)", adapter, property.methodName, out);

    if (property.nullable()) {
      block.endControlFlow();
    }

    return block.build();
  }

  static TypeName getTypeNameFromProperty(AutoValueParcelExtension.Property property, Types types) {
    TypeMirror returnType = property.element.getReturnType();
    if (returnType.getKind() == TypeKind.TYPEVAR) {
      TypeVariable vType = (TypeVariable) returnType;
      returnType = vType.getUpperBound();
    }
    TypeElement element = (TypeElement) types.asElement(returnType);
    if (element != null) {
      TypeName parcelableType = getParcelableType(types, element);
      if (!PARCELABLE.equals(parcelableType) && element.getKind() == ElementKind.ENUM) {
        return ENUM;
      }
      return parcelableType;
    }
    return property.type;
  }

  static boolean isTypeRequiresSuppressWarnings(TypeName type) {
    return type.equals(LIST) ||
            type.equals(MAP);
  }
}
