package com.ryanharter.auto.value.parcel;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.junit.Assert.fail;

import com.google.auto.common.MoreElements;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.CompilationRule;
import com.google.testing.compile.JavaFileObjects;
import android.os.Parcelable;

import com.ryanharter.auto.value.parcel.util.TestMessager;
import com.ryanharter.auto.value.parcel.util.TestProcessingEnvironment;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public class AutoValueParcelExtensionTest {

  @Rule public CompilationRule rule = new CompilationRule();

  AutoValueParcelExtension extension = new AutoValueParcelExtension();

  private Elements elements;
  private ProcessingEnvironment processingEnvironment;

  private JavaFileObject parcelable;
  private JavaFileObject parcel;
  private JavaFileObject nullable;
  private JavaFileObject textUtils;

  @Before public void setup() {
    Messager messager = new TestMessager();
    elements = rule.getElements();
    processingEnvironment = new TestProcessingEnvironment(messager, elements, rule.getTypes());

    parcelable = JavaFileObjects.forSourceString("android.os.Parcelable", ""
        + "package android.os;\n"
        + "public interface Parcelable {\n"
        + "public interface Creator<T> {\n"
        + "  public T createFromParcel(Parcel source);\n"
        + "  public T[] newArray(int size);\n"
        + "}"
        + "int describeContents();\n"
        + "void writeToParcel(Parcel in, int flags);\n"
        + "}\n");
    parcel = JavaFileObjects.forSourceString("android.os.Parcel", "" +
        "package android.os;\n" +
        "import java.util.HashMap;\n" +
        "import java.util.Map;\n" +
        "import java.util.List;\n" +
        "import java.util.ArrayList;\n" +
        "import java.io.Serializable;\n" +
        "import android.util.SparseArray;\n" +
        "import android.util.SparseBooleanArray;\n" +
        "import android.util.Size;\n" +
        "import android.util.SizeF;\n" +
        "public interface Parcel {\n" +
        "Object readValue(ClassLoader cl);\n" +
        "void writeValue(Object o);\n" +
        "  byte readByte();\n" +
        "  int readInt();\n" +
        "  long readLong();\n" +
        "  float readFloat();\n" +
        "  double readDouble();\n" +
        "  String readString();\n" +
        "  Parcelable readParcelable(ClassLoader cl);\n" +
        "  CharSequence readCharSequence();\n" +
        "  HashMap readHashMap(ClassLoader cl);\n" +
        "  ArrayList readArrayList(ClassLoader cl);\n" +
        "  boolean[] createBooleanArray();\n" +
        "  byte[] createByteArray();\n" +
        "  char[] createCharArray();\n" +
        "  int[] createIntArray();\n" +
        "  long[] createLongArray();\n" +
        "  String[] readStringArray();\n" +
        "  Serializable readSerializable();\n" +
        "  SparseArray readSparseArray(ClassLoader cl);\n" +
        "  SparseBooleanArray readSparseBooleanArray();\n" +
        "  Bundle readBundle(ClassLoader cl);\n" +
        "  PersistableBundle readPersistableBundle(ClassLoader cl);\n" +
        "  Size readSize();\n" +
        "  SizeF readSizeF();\n" +
        "  IBinder readStrongBinder();\n" +
        "  void writeString(String in);\n" +
        "  void writeParcelable(Parcelable in, int flags);\n" +
        "  void writeCharSequence(CharSequence in);\n" +
        "  void writeMap(Map in);\n" +
        "  void writeList(List in);\n" +
        "  void writeBooleanArray(boolean[] in);\n" +
        "  void writeByteArray(byte[] in);\n" +
        "  void writeCharArray(char[] in);\n" +
        "  void writeIntArray(int[] in);\n" +
        "  void writeLongArray(long[] in);\n" +
        "  void writeSerializable(Serializable in);\n" +
        "  void writeSparseArray(SparseArray in);\n" +
        "  void writeSparseBooleanArray(SparseBooleanArray in);\n" +
        "  void writeBundle(Bundle in);\n" +
        "  void writePersistableBundle(PersistableBundle in);\n" +
        "  void writeSize(Size in);\n" +
        "  void writeSizeF(SizeF in);\n" +
        "  void writeInt(int in);\n" +
        "  void writeLong(long in);\n" +
        "  void writeFloat(float in);\n" +
        "  void writeDouble(double in);\n" +
        "  void writeStrongBinder(IBinder in);\n"
        + "}");
    nullable = JavaFileObjects.forSourceString("com.ryanharter.auto.value.moshi.Nullable", ""
        + "package test;\n"
        + "import java.lang.annotation.Retention;\n"
        + "import java.lang.annotation.Target;\n"
        + "import static java.lang.annotation.ElementType.METHOD;\n"
        + "import static java.lang.annotation.ElementType.PARAMETER;\n"
        + "import static java.lang.annotation.ElementType.FIELD;\n"
        + "import static java.lang.annotation.RetentionPolicy.SOURCE;\n"
        + "@Retention(SOURCE)\n"
        + "@Target({METHOD, PARAMETER, FIELD})\n"
        + "public @interface Nullable {\n"
        + "}");
    textUtils = JavaFileObjects.forSourceString("android.text.TextUtils", ""
        + "package android.text;\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "public class TextUtils {\n"
        + "public static void writeToParcel(CharSequence cs, Parcel p, int flags) {}\n"
        + "public static final Parcelable.Creator<CharSequence> CHAR_SEQUENCE_CREATOR\n"
        + "= new Parcelable.Creator<CharSequence>() {\n"
        + "@Override public CharSequence createFromParcel(Parcel in) { return null; }\n"
        + "@Override public CharSequence[] newArray(int size) { return null; }\n"
        + "};\n"
        + "}\n");
  }

  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
        + "public abstract int a();\n"
        + "@Nullable public abstract Double b();\n"
        + "public abstract String c();\n"
        + "public abstract long d();\n"
        + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Double;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_Test extends $AutoValue_Test {\n" +
        "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
        "\n" +
        "    @Override\n" +
        "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Test(\n" +
        "          in.readInt(),\n" +
        "          in.readInt() == 0 ? in.readDouble() : null,\n" +
        "          in.readString(),\n" +
        "          in.readLong()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Test[] newArray(int size) {\n" +
        "      return new AutoValue_Test[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Test(int a, Double b, String c, long d) {\n" +
        "    super(a, b, c, d);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeInt(a());\n" +
        "    if (b() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      dest.writeDouble(b());\n" +
        "    }\n" +
        "    dest.writeString(c());\n" +
        "    dest.writeLong(d());\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void propertyMethodReferencedWithPrefix() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.AbstractParcelable", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "public abstract class AbstractParcelable implements Parcelable {\n"
        + "  @Override public final int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}"
    );
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test extends AbstractParcelable {\n"
        + "  public abstract String getName();\n"
        + "  public abstract boolean isAwesome();\n"
        + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_Test extends $AutoValue_Test {\n" +
        "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
        "\n" +
        "    @Override\n" +
        "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Test(\n" +
        "          in.readString(),\n" +
        "          in.readInt() == 1\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Test[] newArray(int size) {\n" +
        "      return new AutoValue_Test[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Test(String name, boolean awesome) {\n" +
        "    super(name, awesome);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeString(getName());\n" +
        "    dest.writeInt(isAwesome() ? 1 : 0);\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, nullable, source1, source2))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void builder() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
        + "public abstract int a();\n"
        + "public abstract Double b();\n"
        + "public abstract String c();\n"
        + "@AutoValue.Builder public static abstract class Builder {\n"
        + "  public abstract Builder a(int a);\n"
        + "  public abstract Builder b(Double b);\n"
        + "  public abstract Builder c(String c);\n"
        + "  public abstract Test build();\n"
        + "}\n"
        + "}"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test public void handlesParcelableCollectionTypes() {
    JavaFileObject parcelable1 = JavaFileObjects.forSourceString("test.Parcelable1", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import android.os.Parcel;\n"
        + "public class Parcelable1 implements Parcelable {\n"
        + "  public int describeContents() {"
        + "    return 0;"
        + "  }\n"
        + "  public void writeToParcel(Parcel in, int flags) {"
        + "  }\n"
        + "  public static final Creator<Parcelable1> CREATOR = new Creator<Parcelable1>() {\n"
        + "    public Parcelable1 createFromParcel(Parcel in) {\n"
        + "      return new Parcelable1();\n"
        + "    }\n"
        + "    public Parcelable1[] newArray(int size) {\n"
        + "      return new Parcelable1[size];\n"
        + "    }\n"
        + "  };"
        + "}");
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import java.util.List;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
          + "public abstract List<Parcelable1> a();\n"
          + "public abstract int[] b();\n"
        + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.SuppressWarnings;\n" +
        "import java.util.List;\n" +
        "\n" +
        "final class AutoValue_Test extends $AutoValue_Test {\n" +
        "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
        "    @Override\n" +
        "    @SuppressWarnings(\"unchecked\")\n" +
        "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Test(\n" +
        "        (List<Parcelable1>) in.readArrayList(Parcelable1.class.getClassLoader()),\n" +
        "        in.createIntArray()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Test[] newArray(int size) {\n" +
        "      return new AutoValue_Test[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Test(List<Parcelable1> a, int[] b) {\n" +
        "    super(a, b);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeList(a());\n" +
        "    dest.writeIntArray(b());\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, parcelable1, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void describeContentsOmittedWhenAlreadyDefined() {
    JavaFileObject notMatching = JavaFileObjects.forSourceString("test.No", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class No implements Parcelable {\n"
        + "  public abstract String name();\n"
        + "  @Override public abstract int describeContents();\n"
        + "  public int describeContents(String name) {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}");
    JavaFileObject matching = JavaFileObjects.forSourceString("test.Yes", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Yes implements Parcelable {\n"
        + "  public abstract String name();\n"
        + "public int describeContents() {\n"
        + "  return 0;\n"
        + "}\n"
        + "}"
    );

    JavaFileObject expectedNotMatching = JavaFileObjects.forSourceString("test/AutoValue_No", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_No extends $AutoValue_No {\n" +
        "  public static final Parcelable.Creator<AutoValue_No> CREATOR = new Parcelable.Creator<AutoValue_No>() {\n" +
        "    @Override\n" +
        "    public AutoValue_No createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_No(\n" +
        "        in.readString()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_No[] newArray(int size) {\n" +
        "      return new AutoValue_No[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_No(String name) {\n" +
        "    super(name);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeString(name());\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    JavaFileObject expectedMatching = JavaFileObjects.forSourceString("test/AutoValue_Yes", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_Yes extends $AutoValue_Yes {\n" +
        "  public static final Parcelable.Creator<AutoValue_Yes> CREATOR = new Parcelable.Creator<AutoValue_Yes>() {\n" +
        "    @Override\n" +
        "    public AutoValue_Yes createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Yes(\n" +
        "        in.readString()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Yes[] newArray(int size) {\n" +
        "      return new AutoValue_Yes[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Yes(String name) {\n" +
        "    super(name);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeString(name());\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, notMatching, matching))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedNotMatching, expectedMatching);
  }

  @Test public void failWhenWriteToParcelAlreadyDefinedTest() throws Exception {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
        + "  public abstract String name();\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, source))
        .processedWith(new AutoValueProcessor())
        .failsToCompile()
        .withErrorContaining("Manual implementation of Parcelable#writeToParcel(Parcel,int) found when "
                             + "processing test.Test. Remove this so auto-value-parcel can automatically "
                             + "generate the implementation for you.")
        .in(source)
        .onLine(8);
  }

  @Test public void failWhenCreatorAlreadyDefinedTest() throws Exception {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
        + "  public abstract String name();\n"
        + "  public static final Parcelable.Creator<Test> CREATOR = new Parcelable.Creator<Test>() {\n"
        + "    @Override public Test createFromParcel(Parcel in) {\n"
        + "      return null;\n"
        + "    }\n"
        + "    @Override public Test[] newArray(int size) {\n"
        + "      return new Test[size];\n"
        + "    }\n"
        + "  };\n"
        + "}"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, source))
        .processedWith(new AutoValueProcessor())
        .failsToCompile()
        .withErrorContaining("Manual implementation of a static Parcelable.Creator<T> CREATOR field "
                             + "found when processing test.Test. Remove this so auto-value-parcel can "
                             + "automatically generate the implementation for you.")
        .in(source)
        .onLine(7);
  }

  @Test public void handlesAllParcelableTypes() {
    JavaFileObject parcelable1 = JavaFileObjects.forSourceString("test.Parcelable1", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import android.os.Parcel;\n"
        + "public class Parcelable1 implements Parcelable {\n"
        + "  public int describeContents() {"
        + "    return 0;"
        + "  }\n"
        + "  public void writeToParcel(Parcel in, int flags) {"
        + "  }\n"
        + "  public static final Creator<Parcelable1> CREATOR = new Creator<Parcelable1>() {\n"
        + "    public Parcelable1 createFromParcel(Parcel in) {\n"
        + "      return new Parcelable1();\n"
        + "    }\n"
        + "    public Parcelable1[] newArray(int size) {\n"
        + "      return new Parcelable1[size];\n"
        + "    }\n"
        + "  };"
        + "}");
    JavaFileObject foobinder = JavaFileObjects.forSourceString("test.FooBinder", "" +
        "package test;\n" +
        "import android.os.IBinder;\n" +
        "public class FooBinder implements IBinder {\n" +
        "}\n");
    JavaFileObject numbersEnum = JavaFileObjects.forSourceString("test.Numbers", "" +
        "package test;\n" +
        "public enum Numbers {\n" +
        "  ONE, TWO, THREE\n" +
        "}\n");
    JavaFileObject parcelableEnum = JavaFileObjects.forSourceString("test.Numbers2", "" +
        "package test;\n" +
        "import android.os.Parcelable;\n" +
        "import android.os.Parcel;\n" +
        "public enum Numbers2 implements Parcelable {\n" +
        "  ONE, TWO, THREE;\n" +
        "  public int describeContents() {" +
        "    return 0;" +
        "  }\n" +
        "  public void writeToParcel(Parcel in, int flags) {" +
        "  }\n" +
        "  public static final Creator<Numbers2> CREATOR = new Creator<Numbers2>() {\n" +
        "    public Numbers2 createFromParcel(Parcel in) {\n" +
        "      return Numbers2.ONE;\n" +
        "    }\n" +
        "    public Numbers2[] newArray(int size) {\n" +
        "      return new Numbers2[size];\n" +
        "    }\n" +
        "  };" +
        "}\n");
    JavaFileObject source = JavaFileObjects.forSourceString("test.Foo", "" +
        "package test;\n" +
        "\n" +
        "import android.os.Bundle;\n" +
        "import android.os.IBinder;\n" +
        "import android.os.Parcelable;\n" +
        "import android.os.PersistableBundle;\n" +
        "import android.util.SizeF;\n" +
        "import android.util.Size;\n" +
        "import android.util.SparseArray;\n" +
        "import android.util.SparseBooleanArray;\n" +
        "import com.google.auto.value.AutoValue;\n" +
        "import java.io.Serializable;\n" +
        "import java.util.List;\n" +
        "import java.util.Map;\n" +
        "\n" +
        "@AutoValue public abstract class Foo implements Parcelable {\n" +
        "  @Nullable public abstract String a();\n" +
        "  public abstract byte b();\n" +
        "  public abstract Byte B();\n" +
        "  public abstract int c();\n" +
        "  public abstract Integer C();\n" +
        "  public abstract short d();\n" +
        "  public abstract Short D();\n" +
        "  public abstract long e();\n" +
        "  public abstract Long E();\n" +
        "  public abstract float f();\n" +
        "  public abstract Float F();\n" +
        "  public abstract double g();\n" +
        "  public abstract Double G();\n" +
        "  public abstract boolean h();\n" +
        "  public abstract Boolean H();\n" +
        "  public abstract Parcelable i();\n" +
        "  public abstract CharSequence j();\n" +
        "  public abstract Map<String, String> k();\n" +
        "  public abstract List<String> l();\n" +
        "  public abstract boolean[] m();\n" +
        "  public abstract byte[] n();\n" +
        "  public abstract int[] s();\n" +
        "  public abstract long[] t();\n" +
        "  public abstract Serializable u();\n" +
        "  public abstract SparseArray w();\n" +
        "  public abstract SparseBooleanArray x();\n" +
        "  public abstract Bundle y();\n" +
        "  public abstract PersistableBundle z();\n" +
        "  public abstract Size aa();\n" +
        "  public abstract SizeF ab();\n" +
        "  @Nullable public abstract Parcelable1 ad();\n" +
        "  public abstract FooBinder ae();\n" +
        "  @Nullable public abstract Boolean af();\n" +
        "  public abstract char ag();\n" +
        "  public abstract Character ah();\n" +
        "  public abstract char[] ai();\n" +
        "  public abstract Numbers aj();\n" +
        "  public abstract Numbers2 ak();\n" +
        "}");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", "" +
        "package test;\n" +
        "\n" +
        "import android.os.Bundle;\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import android.os.PersistableBundle;\n" +
        "import android.text.TextUtils;\n" +
        "import android.util.Size;\n" +
        "import android.util.SizeF;\n" +
        "import android.util.SparseArray;\n" +
        "import android.util.SparseBooleanArray;\n" +
        "import java.io.Serializable;\n" +
        "import java.lang.Boolean;\n" +
        "import java.lang.Byte;\n" +
        "import java.lang.CharSequence;\n" +
        "import java.lang.Character;\n" +
        "import java.lang.Double;\n" +
        "import java.lang.Float;\n" +
        "import java.lang.Integer;\n" +
        "import java.lang.Long;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.Short;\n" +
        "import java.lang.String;\n" +
        "import java.lang.SuppressWarnings;\n" +
        "import java.util.List;\n" +
        "import java.util.Map;\n" +
        "\n" +
        "final class AutoValue_Foo extends $AutoValue_Foo {\n" +
        "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n" +
        "    @Override\n" +
        "    @SuppressWarnings(\"unchecked\")\n" +
        "    public AutoValue_Foo createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Foo(\n" +
        "        in.readInt() == 0 ? in.readString() : null,\n" +
        "        in.readByte(),\n" +
        "        in.readByte(),\n" +
        "        in.readInt(),\n" +
        "        in.readInt(),\n" +
        "        (short) in.readInt(),\n" +
        "        (short) in.readInt(),\n" +
        "        in.readLong(),\n" +
        "        in.readLong(),\n" +
        "        in.readFloat(),\n" +
        "        in.readFloat(),\n" +
        "        in.readDouble(),\n" +
        "        in.readDouble(),\n" +
        "        in.readInt() == 1,\n" +
        "        in.readInt() == 1,\n" +
        "        in.readParcelable(Parcelable.class.getClassLoader()),\n" +
        "        TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in),\n" +
        "        (Map<String, String>) in.readHashMap(String.class.getClassLoader()),\n" +
        "        (List<String>) in.readArrayList(String.class.getClassLoader()),\n" +
        "        in.createBooleanArray(),\n" +
        "        in.createByteArray(),\n" +
        "        in.createIntArray(),\n" +
        "        in.createLongArray(),\n" +
        "        in.readSerializable(),\n" +
        "        in.readSparseArray(SparseArray.class.getClassLoader()),\n" +
        "        in.readSparseBooleanArray(),\n" +
        "        in.readBundle(Bundle.class.getClassLoader()),\n" +
        "        in.readPersistableBundle(PersistableBundle.class.getClassLoader()),\n" +
        "        in.readSize(),\n" +
        "        in.readSizeF(),\n" +
        "        in.readInt() == 0 ? Parcelable1.CREATOR.createFromParcel(in) : null,\n" +
        "        (FooBinder) in.readStrongBinder(),\n" +
        "        in.readInt() == 0 ? in.readInt() == 1 : null,\n" +
        "        (char) in.readInt(),\n" +
        "        (char) in.readInt(),\n" +
        "        in.createCharArray(),\n" +
        "        Numbers.valueOf(in.readString()),\n" +
        "        Numbers2.CREATOR.createFromParcel(in)\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Foo[] newArray(int size) {\n" +
        "      return new AutoValue_Foo[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Foo(String a, byte b, Byte B, int c, Integer C, short d, Short D, long e, Long E, float f, Float F, double g, Double G, boolean h, Boolean H, Parcelable i, CharSequence j, Map<String, String> k, List<String> l, boolean[] m, byte[] n, int[] s, long[] t, Serializable u, SparseArray w, SparseBooleanArray x, Bundle y, PersistableBundle z, Size aa, SizeF ab, Parcelable1 ad, FooBinder ae, Boolean af, char ag, Character ah, char[] ai, Numbers aj, Numbers2 ak) {\n" +
        "    super(a, b, B, c, C, d, D, e, E, f, F, g, G, h, H, i, j, k, l, m, n, s, t, u, w, x, y, z, aa, ab, ad, ae, af, ag, ah, ai, aj, ak);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    if (a() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      dest.writeString(a());\n" +
        "    }\n" +
        "    dest.writeInt(b());\n" +
        "    dest.writeInt(B());\n" +
        "    dest.writeInt(c());\n" +
        "    dest.writeInt(C());\n" +
        "    dest.writeInt(((Short) d()).intValue());\n" +
        "    dest.writeInt(D().intValue());\n" +
        "    dest.writeLong(e());\n" +
        "    dest.writeLong(E());\n" +
        "    dest.writeFloat(f());\n" +
        "    dest.writeFloat(F());\n" +
        "    dest.writeDouble(g());\n" +
        "    dest.writeDouble(G());\n" +
        "    dest.writeInt(h() ? 1 : 0);\n" +
        "    dest.writeInt(H() ? 1 : 0);\n" +
        "    dest.writeParcelable(i(), flags);\n" +
        "    TextUtils.writeToParcel(j(), dest, flags);\n" +
        "    dest.writeMap(k());\n" +
        "    dest.writeList(l());\n" +
        "    dest.writeBooleanArray(m());\n" +
        "    dest.writeByteArray(n());\n" +
        "    dest.writeIntArray(s());\n" +
        "    dest.writeLongArray(t());\n" +
        "    dest.writeSerializable(u());\n" +
        "    dest.writeSparseArray(w());\n" +
        "    dest.writeSparseBooleanArray(x());\n" +
        "    dest.writeBundle(y());\n" +
        "    dest.writePersistableBundle(z());\n" +
        "    dest.writeSize(aa());\n" +
        "    dest.writeSizeF(ab());\n" +
        "    if (ad() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      ad().writeToParcel(dest, flags);\n" +
        "    }\n" +
        "    dest.writeStrongBinder(ae());\n" +
        "    if (af() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      dest.writeInt(af() ? 1 : 0);\n" +
        "    }\n" +
        "    dest.writeInt(ag());\n" +
        "    dest.writeInt(ah());\n" +
        "    dest.writeCharArray(ai());\n" +
        "    dest.writeString(aj().name());\n" +
        "    ak().writeToParcel(dest, flags);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(nullable, parcel, parcelable, textUtils, parcelable1, foobinder, numbersEnum, parcelableEnum, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void usesProperClassLoader() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import java.util.Map;\n"
        + "import java.util.List;\n"
        + "import java.lang.CharSequence;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
        + "public abstract Map a();\n"
        + "public abstract Map<String, CharSequence> b();\n"
        + "public abstract Map<CharSequence, String> c();\n"
        + "public abstract List d();\n"
        + "public abstract List<String> e();\n"
        + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.CharSequence;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "import java.lang.SuppressWarnings;\n" +
        "import java.util.List;\n" +
        "import java.util.Map;\n" +
        "\n" +
        "final class AutoValue_Test extends $AutoValue_Test {\n" +
        "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
        "\n" +
        "    @Override\n" +
        "    @SuppressWarnings(\"unchecked\")" +
        "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Test(\n" +
        "          (Map) in.readHashMap(Map.class.getClassLoader()),\n" +
        "          (Map<String, CharSequence>) in.readHashMap(CharSequence.class.getClassLoader()),\n" +
        "          (Map<CharSequence, String>) in.readHashMap(String.class.getClassLoader()),\n" +
        "          (List) in.readArrayList(List.class.getClassLoader()),\n" +
        "          (List<String>) in.readArrayList(String.class.getClassLoader())" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Test[] newArray(int size) {\n" +
        "      return new AutoValue_Test[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Test(Map a, Map<String, CharSequence> b, Map<CharSequence, String> c, List d, List<String> e) {\n" +
        "    super(a, b, c, d, e);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeMap(a());\n" +
        "    dest.writeMap(b());\n" +
        "    dest.writeMap(c());\n" +
        "    dest.writeList(d());\n" +
        "    dest.writeList(e());\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void throwsForNonParcelableProperty() throws Exception {
    TypeElement type = elements.getTypeElement(SampleTypeWithNonSerializable.class.getCanonicalName());
    AutoValueExtension.Context context = createContext(type);

    try {
      extension.generateClass(context, "Test_AnnotatedType", "SampleTypeWithNonSerializable", true);
      fail();
    } catch (AutoValueParcelException e) {}
  }

  @Test public void acceptsParcelableProperties() throws Exception {
    TypeElement type = elements.getTypeElement(SampleTypeWithParcelable.class.getCanonicalName());
    AutoValueExtension.Context context = createContext(type);

    String generated = extension.generateClass(context, "Test_TypeWithParcelable", "SampleTypeWithParcelable", true);
    assertThat(generated).isNotNull();
  }

  @Test public void usesCustomParcelTypeAdapter() throws Exception {
    JavaFileObject bar = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import java.util.Date;\n"
        + "public class Bar {\n"
        + "  public Date date;\n"
        + "  public boolean valid;\n"
        + "  public Bar(Date date, boolean valid) {\n"
        + "    this.date = date;\n"
        + "    this.valid = valid;\n"
        + "  }\n"
        + "}");
    JavaFileObject barAdapter = JavaFileObjects.forSourceString("test.BarTypeAdapter", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "import java.util.Date;\n"
        + "import com.ryanharter.auto.value.parcel.TypeAdapter;\n"
        + "public class BarTypeAdapter implements TypeAdapter<Bar> {\n"
        + "\n"
        + "  public Bar fromParcel(Parcel in) {\n"
        + "    return new Bar(\n"
        + "        new Date(in.readLong()),\n"
        + "        in.readInt() == 1);\n"
        + "  }\n"
        + "\n"
        + "  public void toParcel(Bar value, Parcel dest) {\n"
        + "    dest.writeLong(value.date.getTime());\n"
        + "    dest.writeInt(value.valid ? 1 : 0);\n"
        + "  }\n"
        + "}\n");
    JavaFileObject foo = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.ryanharter.auto.value.parcel.ParcelAdapter;\n"
        + "@AutoValue public abstract class Foo implements Parcelable {\n"
        + "  @ParcelAdapter(BarTypeAdapter.class) public abstract Bar bar();\n"
        + "  @ParcelAdapter(BarTypeAdapter.class) public abstract Bar bar1();\n"
        + "}\n");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", ""
        + "package test;\n"
        + "\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "final class AutoValue_Foo extends $AutoValue_Foo {\n"
        + "\n"
        + "  private static final BarTypeAdapter BAR_TYPE_ADAPTER = new BarTypeAdapter();\n"
        + "\n"
        + "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n"
        + "    @Override\n"
        + "    public AutoValue_Foo createFromParcel(Parcel in) {\n"
        + "      return new AutoValue_Foo(\n"
        + "        BAR_TYPE_ADAPTER.fromParcel(in),\n"
        + "        BAR_TYPE_ADAPTER.fromParcel(in)\n"
        + "      );\n"
        + "    }\n"
        + "    @Override\n"
        + "    public AutoValue_Foo[] newArray(int size) {\n"
        + "      return new AutoValue_Foo[size];\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  AutoValue_Foo(Bar bar, Bar bar1) {\n"
        + "    super(bar, bar1);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "    BAR_TYPE_ADAPTER.toParcel(bar(), dest);\n"
        + "    BAR_TYPE_ADAPTER.toParcel(bar1(), dest);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, bar, barAdapter, foo))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void handlesNestedTypeAdaptersOfSameName() throws Exception {
    JavaFileObject bar = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import java.util.Date;\n"
        + "import android.os.Parcel;\n"
        + "import com.ryanharter.auto.value.parcel.TypeAdapter;\n"
        + "public class Bar {\n"
        + "  public Date date;\n"
        + "  public boolean valid;\n"
        + "  public Bar(Date date, boolean valid) {\n"
        + "    this.date = date;\n"
        + "    this.valid = valid;\n"
        + "  }\n"
        + "\n"
        + "  public static class MyTypeAdapter implements TypeAdapter<Bar> {\n"
        + "\n"
        + "    public Bar fromParcel(Parcel in) {\n"
        + "      return new Bar(\n"
        + "          new Date(in.readLong()),\n"
        + "          in.readInt() == 1);\n"
        + "    }\n"
        + "\n"
        + "    public void toParcel(Bar value, Parcel dest) {\n"
        + "      dest.writeLong(value.date.getTime());\n"
        + "      dest.writeInt(value.valid ? 1 : 0);\n"
        + "    }\n"
        + "  }\n"
        + "}");
    JavaFileObject baz = JavaFileObjects.forSourceString("test.Baz", ""
        + "package test;\n"
        + "import java.util.Date;\n"
        + "import android.os.Parcel;\n"
        + "import com.ryanharter.auto.value.parcel.TypeAdapter;\n"
        + "public class Baz {\n"
        + "  public Date date;\n"
        + "  public boolean valid;\n"
        + "  public Baz(Date date, boolean valid) {\n"
        + "    this.date = date;\n"
        + "    this.valid = valid;\n"
        + "  }\n"
        + "\n"
        + "  public static class MyTypeAdapter implements TypeAdapter<Baz> {\n"
        + "\n"
        + "    public Baz fromParcel(Parcel in) {\n"
        + "      return new Baz(\n"
        + "          new Date(in.readLong()),\n"
        + "          in.readInt() == 1);\n"
        + "    }\n"
        + "\n"
        + "    public void toParcel(Baz value, Parcel dest) {\n"
        + "      dest.writeLong(value.date.getTime());\n"
        + "      dest.writeInt(value.valid ? 1 : 0);\n"
        + "    }\n"
        + "  }\n"
        + "}");
    JavaFileObject foo = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.ryanharter.auto.value.parcel.ParcelAdapter;\n"
        + "@AutoValue public abstract class Foo implements Parcelable {\n"
        + "  @ParcelAdapter(Bar.MyTypeAdapter.class) public abstract Bar bar();\n"
        + "  @ParcelAdapter(Baz.MyTypeAdapter.class) public abstract Baz baz();\n"
        + "}\n");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", ""
        + "package test;\n"
        + "\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "final class AutoValue_Foo extends $AutoValue_Foo {\n"
        + "\n"
        + "  private static final Bar.MyTypeAdapter MY_TYPE_ADAPTER = new Bar.MyTypeAdapter();\n"
        + "  private static final Baz.MyTypeAdapter MY_TYPE_ADAPTER_ = new Baz.MyTypeAdapter();\n"
        + "\n"
        + "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n"
        + "    @Override\n"
        + "    public AutoValue_Foo createFromParcel(Parcel in) {\n"
        + "      return new AutoValue_Foo(\n"
        + "        MY_TYPE_ADAPTER.fromParcel(in),\n"
        + "        MY_TYPE_ADAPTER_.fromParcel(in)\n"
        + "      );\n"
        + "    }\n"
        + "    @Override\n"
        + "    public AutoValue_Foo[] newArray(int size) {\n"
        + "      return new AutoValue_Foo[size];\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  AutoValue_Foo(Bar bar, Baz baz) {\n"
        + "    super(bar, baz);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "    MY_TYPE_ADAPTER.toParcel(bar(), dest);\n"
        + "    MY_TYPE_ADAPTER_.toParcel(baz(), dest);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, bar, baz, foo))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void handlesNullableWithParcelTypeAdapter() throws Exception {
    JavaFileObject bar = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import java.util.Date;\n"
        + "public class Bar {\n"
        + "  public Date date;\n"
        + "  public boolean valid;\n"
        + "  public Bar(Date date, boolean valid) {\n"
        + "    this.date = date;\n"
        + "    this.valid = valid;\n"
        + "  }\n"
        + "}");
    JavaFileObject barAdapter = JavaFileObjects.forSourceString("test.BarTypeAdapter", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "import java.util.Date;\n"
        + "import com.ryanharter.auto.value.parcel.TypeAdapter;\n"
        + "public class BarTypeAdapter implements TypeAdapter<Bar> {\n"
        + "\n"
        + "  public Bar fromParcel(Parcel in) {\n"
        + "    return new Bar(\n"
        + "        new Date(in.readLong()),\n"
        + "        in.readInt() == 1);\n"
        + "  }\n"
        + "\n"
        + "  public void toParcel(Bar value, Parcel dest) {\n"
        + "    dest.writeLong(value.date.getTime());\n"
        + "    dest.writeInt(value.valid ? 1 : 0);\n"
        + "  }\n"
        + "}\n");
    JavaFileObject foo = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.ryanharter.auto.value.parcel.ParcelAdapter;\n"
        + "@AutoValue public abstract class Foo implements Parcelable {\n"
        + "  @Nullable @ParcelAdapter(BarTypeAdapter.class) public abstract Bar barNullable();\n"
        + "  @ParcelAdapter(BarTypeAdapter.class) public abstract Bar barNonNullable();\n"
        + "}\n");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", ""
        + "package test;\n"
        + "\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "final class AutoValue_Foo extends $AutoValue_Foo {\n"
        + "\n"
        + "  private static final BarTypeAdapter BAR_TYPE_ADAPTER = new BarTypeAdapter();\n"
        + "\n"
        + "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n"
        + "    @Override\n"
        + "    public AutoValue_Foo createFromParcel(Parcel in) {\n"
        + "      return new AutoValue_Foo(\n"
        + "        in.readInt() == 0 ? BAR_TYPE_ADAPTER.fromParcel(in) : null,\n"
        + "        BAR_TYPE_ADAPTER.fromParcel(in)\n"
        + "      );\n"
        + "    }\n"
        + "    @Override\n"
        + "    public AutoValue_Foo[] newArray(int size) {\n"
        + "      return new AutoValue_Foo[size];\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  AutoValue_Foo(Bar barNullable, Bar barNonNullable) {\n"
        + "    super(barNullable, barNonNullable);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "    if (barNullable() == null) {\n"
        + "      dest.writeInt(1);\n"
        + "    } else {\n"
        + "      dest.writeInt(0);\n"
        + "      BAR_TYPE_ADAPTER.toParcel(barNullable(), dest);\n"
        + "    }\n"
        + "    BAR_TYPE_ADAPTER.toParcel(barNonNullable(), dest);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n");

    assertAbout(javaSources())
        .that(Arrays.asList(nullable, parcel, parcelable, bar, barAdapter, foo))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void acceptsParcelableSubclassesTwiceRemoved() throws Exception {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.SafeParcelable", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "public interface SafeParcelable extends Parcelable {\n"
        + "}");
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Param", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "public class Param implements SafeParcelable {\n"
        + "  public int describeContents() { return 0; }\n"
        + "  public void writeToParcel(Parcel p, int i) {}\n"
        + "  public static final Creator<Param> CREATOR = null;\n"
        + "}\n");
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Foo implements Parcelable {\n"
        + "  public abstract Param param();\n"
        + "}\n");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", ""
        + "package test;\n"
        + "\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "final class AutoValue_Foo extends $AutoValue_Foo {\n"
        + "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n"
        + "    @Override\n"
        + "    public AutoValue_Foo createFromParcel(Parcel in) {\n"
        + "      return new AutoValue_Foo(\n"
        + "          Param.CREATOR.createFromParcel(in)\n"
        + "      );\n"
        + "    }\n"
        + "    @Override\n"
        + "    public AutoValue_Foo[] newArray(int size) {\n"
        + "      return new AutoValue_Foo[size];\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  AutoValue_Foo(Param param) {\n"
        + "    super(param);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "    param().writeToParcel(dest, flags);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n");

    assertAbout(javaSources())
        .that(Arrays.asList(parcelable, parcel, source1, source2, source3))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void addsSuppressWarningsAnnotationWhenListFieldExists() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import android.os.Parcelable;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "import java.util.List;\n"
            + "@AutoValue public abstract class Test implements Parcelable {\n"
            + "public abstract List<String> a();\n"
            + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n" +
            "\n" +
            "import android.os.Parcel;\n" +
            "import android.os.Parcelable;\n" +
            "import java.lang.Override;\n" +
            "import java.lang.String;\n" +
            "import java.lang.SuppressWarnings;\n" +
            "import java.util.List;\n" +
            "\n" +
            "final class AutoValue_Test extends $AutoValue_Test {\n" +
            "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
            "    @Override\n" +
            "    @SuppressWarnings(\"unchecked\")\n" +
            "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
            "      return new AutoValue_Test(\n" +
            "          (List<String>) in.readArrayList(String.class.getClassLoader())\n" +
            "      );\n" +
            "    }\n" +
            "    @Override\n" +
            "    public AutoValue_Test[] newArray(int size) {\n" +
            "      return new AutoValue_Test[size];\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  AutoValue_Test(List<String> a) {\n" +
            "    super(a);\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public void writeToParcel(Parcel dest, int flags) {\n" +
            "    dest.writeList(a());\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public int describeContents() {\n" +
            "    return 0;\n" +
            "  }\n" +
            "}");

    assertAbout(javaSources())
            .that(Arrays.asList(parcel, parcelable, source))
            .processedWith(new AutoValueProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);
  }

  @Test public void addsSuppressWarningsAnnotationWhenMapFieldExists() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import android.os.Parcelable;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "import java.util.Map;\n"
            + "@AutoValue public abstract class Test implements Parcelable {\n"
            + "public abstract Map<String, String> a();\n"
            + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n" +
            "\n" +
            "import android.os.Parcel;\n" +
            "import android.os.Parcelable;\n" +
            "import java.lang.Override;\n" +
            "import java.lang.String;\n" +
            "import java.lang.SuppressWarnings;\n" +
            "import java.util.Map;\n" +
            "\n" +
            "final class AutoValue_Test extends $AutoValue_Test {\n" +
            "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
            "    @Override\n" +
            "    @SuppressWarnings(\"unchecked\")\n" +
            "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
            "      return new AutoValue_Test(\n" +
            "          (Map<String, String>) in.readHashMap(String.class.getClassLoader())\n" +
            "      );\n" +
            "    }\n" +
            "    @Override\n" +
            "    public AutoValue_Test[] newArray(int size) {\n" +
            "      return new AutoValue_Test[size];\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  AutoValue_Test(Map<String, String> a) {\n" +
            "    super(a);\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public void writeToParcel(Parcel dest, int flags) {\n" +
            "    dest.writeMap(a());\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public int describeContents() {\n" +
            "    return 0;\n" +
            "  }\n" +
            "}");

    assertAbout(javaSources())
            .that(Arrays.asList(parcel, parcelable, source))
            .processedWith(new AutoValueProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);
  }

  @Test public void doesNotAddSuppressWarningsAnnotationWithoutMapOrList() {

    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import android.os.Parcelable;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "import java.lang.CharSequence;\n"
            + "import android.os.IBinder;\n"
            + "import java.io.Serializable;\n"
            + "import android.util.Size;\n"
            + "import android.util.SizeF;\n"
            + "import android.os.PersistableBundle;\n"

            + "@AutoValue public abstract class Test implements Parcelable {\n"
            + "public abstract byte a();\n"
            + "public abstract int b();\n"
            + "public abstract short c();\n"
            + "public abstract long d();\n"
            + "public abstract float e();\n"
            + "public abstract double f();\n"
            + "public abstract CharSequence g();\n"
            + "public abstract IBinder i();\n"
            + "public abstract String j();\n"
            + "public abstract boolean k();\n"
            + "public abstract boolean[] l();\n"
            + "public abstract byte[] m();\n"
            + "public abstract int[] o();\n"
            + "public abstract long[] p();\n"
            + "public abstract Serializable q();\n"
            + "public abstract Size s();\n"
            + "public abstract SizeF t();\n"
            + "public abstract PersistableBundle u();\n"

            + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            +  "package test;\n" +
            "\n" +
            "import android.os.IBinder;\n" +
            "import android.os.Parcel;\n" +
            "import android.os.Parcelable;\n" +
            "import android.os.PersistableBundle;\n" +
            "import android.text.TextUtils;\n" +
            "import android.util.Size;\n" +
            "import android.util.SizeF;\n" +
            "import java.io.Serializable;\n" +
            "import java.lang.CharSequence;\n" +
            "import java.lang.Override;\n" +
            "import java.lang.String;\n" +
            "\n" +
            "final class AutoValue_Test extends $AutoValue_Test {\n" +
            "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
            "    @Override\n" +
            "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
            "      return new AutoValue_Test(\n" +
            "          in.readByte(),\n" +
            "          in.readInt(),\n" +
            "          (short) in.readInt(),\n" +
            "          in.readLong(),\n" +
            "          in.readFloat(),\n" +
            "          in.readDouble(),\n" +
            "          TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in),\n" +
            "          in.readStrongBinder(),\n" +
            "          in.readString(),\n" +
            "          in.readInt() == 1,\n" +
            "          in.createBooleanArray(),\n" +
            "          in.createByteArray(),\n" +
            "          in.createIntArray(),\n" +
            "          in.createLongArray(),\n" +
            "          in.readSerializable(),\n" +
            "          in.readSize(),\n" +
            "          in.readSizeF(),\n" +
            "          in.readPersistableBundle(PersistableBundle.class.getClassLoader())\n" +
            "      );\n" +
            "    }\n" +
            "    @Override\n" +
            "    public AutoValue_Test[] newArray(int size) {\n" +
            "      return new AutoValue_Test[size];\n" +
            "    }\n" +
            "  };\n" +
            "\n" +
            "  AutoValue_Test(byte a, int b, short c, long d, float e, double f, CharSequence g, IBinder i, String j, boolean k, boolean[] l, byte[] m, int[] o, long[] p, Serializable q, Size s, SizeF t, PersistableBundle u) {\n" +
            "    super(a, b, c, d, e, f, g, i, j, k, l, m, o, p, q, s, t, u);\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public void writeToParcel(Parcel dest, int flags) {\n" +
            "    dest.writeInt(a());\n" +
            "    dest.writeInt(b());\n" +
            "    dest.writeInt(((Short) c()).intValue());\n" +
            "    dest.writeLong(d());\n" +
            "    dest.writeFloat(e());\n" +
            "    dest.writeDouble(f());\n" +
            "    TextUtils.writeToParcel(g(), dest, flags);\n" +
            "    dest.writeStrongBinder(i());\n" +
            "    dest.writeString(j());\n" +
            "    dest.writeInt(k() ? 1 : 0);\n" +
            "    dest.writeBooleanArray(l());\n" +
            "    dest.writeByteArray(m());\n" +
            "    dest.writeIntArray(o());\n" +
            "    dest.writeLongArray(p());\n" +
            "    dest.writeSerializable(q());\n" +
            "    dest.writeSize(s());\n" +
            "    dest.writeSizeF(t());\n" +
            "    dest.writePersistableBundle(u());\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public int describeContents() {\n" +
            "    return 0;\n" +
            "  }\n" +
            "}");

    assertAbout(javaSources())
            .that(Arrays.asList(parcel, parcelable, textUtils, source))
            .processedWith(new AutoValueProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);

  }

  private AutoValueExtension.Context createContext(TypeElement type) {
    String packageName = MoreElements.getPackage(type).getQualifiedName().toString();
    Set<ExecutableElement> allMethods = MoreElements.getLocalAndInheritedMethods(type, elements);
    Set<ExecutableElement> methods = methodsToImplement(type, allMethods);
    Map<String, ExecutableElement> properties = new LinkedHashMap<String, ExecutableElement>();
    for (ExecutableElement e : methods) {
      properties.put(e.getSimpleName().toString(), e);
    }

    return new TestContext(processingEnvironment, packageName, type, properties);
  }

  private static class TestContext implements AutoValueExtension.Context {

    private final ProcessingEnvironment processingEnvironment;
    private final String packageName;
    private final TypeElement autoValueClass;
    private final Map<String, ExecutableElement> properties;

    public TestContext(ProcessingEnvironment processingEnvironment, String packageName,
        TypeElement autoValueClass, Map<String, ExecutableElement> properties) {
      this.processingEnvironment = processingEnvironment;
      this.packageName = packageName;
      this.autoValueClass = autoValueClass;
      this.properties = properties;
    }

    @Override public ProcessingEnvironment processingEnvironment() {
      return processingEnvironment;
    }

    @Override public String packageName() {
      return packageName;
    }

    @Override public TypeElement autoValueClass() {
      return autoValueClass;
    }

    @Override public Map<String, ExecutableElement> properties() {
      return properties;
    }

    @Override public Set<ExecutableElement> abstractMethods() {
      return Collections.emptySet();
    }
  }

  abstract class SampleTypeWithNonSerializable implements Parcelable {
    abstract int primitive();
    abstract String serializable();
    abstract NonSerializable nonSerializable();
  }

  abstract class SampleTypeWithParcelable implements Parcelable {
    abstract int primitive();
    abstract String serializable();
    abstract ParcelableProperty parcelable();
  }

  abstract class ParcelableProperty implements Parcelable {}

  class NonSerializable {}

  private ImmutableSet<ExecutableElement> methodsToImplement(
      TypeElement autoValueClass, Set<ExecutableElement> methods) {
    ImmutableSet.Builder<ExecutableElement> toImplement = ImmutableSet.builder();
    for (ExecutableElement method : methods) {
      if (method.getModifiers().contains(Modifier.ABSTRACT)
          && !Arrays.asList("toString", "hashCode", "equals").contains(method.getSimpleName().toString())) {
        if (method.getParameters().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID) {
          toImplement.add(method);
        }
      }
    }
    return toImplement.build();
  }

}
