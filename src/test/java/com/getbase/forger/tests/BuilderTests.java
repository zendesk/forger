package com.getbase.forger.tests;

import com.getbase.forger.FakeDataGenerator;
import com.getbase.forger.Forger;

import org.chalup.microorm.MicroOrm;
import org.chalup.microorm.TypeAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentValues;
import android.database.Cursor;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BuilderTests {

  @Test
  public void shouldMakeSomeUseFromFakeGeneratorForCustomType() throws Exception {
    final MicroOrm microOrm = new MicroOrm.Builder()
        .registerTypeAdapter(TestModels.ComplexDate.class, new CustomDateAdapter())
        .build();

    final Forger<TestModels.TestModel> forger = Forger.<TestModels.TestModel>builder()
        .withMicroOrm(microOrm)
        .withModelGraph(TestModels.MODEL_GRAPH)
        .registerCustomGenerator(TestModels.ComplexDate.class, new CustomDateAdapter())
        .build();

    final TestModels.ModelWithComplexDate date
        = forger.iNeed(TestModels.ModelWithComplexDate.class).in(EchoContentResolver.get());

    assertThat(date.getComplexDate().getTimestamp()).isEqualTo(CustomDateAdapter.TIMESTAMP_TO_RETURN);
    assertThat(date.getAnotherField()).isNotNull();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowPuttingCustomGeneratorForOneClassTwice() throws Exception {
    Forger.<TestModels.TestModel>builder()
        .registerCustomGenerator(TestModels.ComplexDate.class, new CustomDateAdapter())
        .registerCustomGenerator(TestModels.ComplexDate.class, new CustomDateAdapter());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowPuttingNullAsGenerator() throws Exception {
    Forger.<TestModels.TestModel>builder()
        .registerCustomGenerator(TestModels.ComplexDate.class, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowPuttingNullAsClass() throws Exception {
    Forger.<TestModels.TestModel>builder()
        .registerCustomGenerator(null, new CustomDateAdapter());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowBuildingWithoutMicroOrm() throws Exception {
    Forger.<TestModels.TestModel>builder()
        .withModelGraph(TestModels.MODEL_GRAPH)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowBuildingWithoutModelGraph() throws Exception {
    Forger.<TestModels.TestModel>builder()
        .withMicroOrm(new MicroOrm())
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowPuttingMicroOrmTwice() throws Exception {
    Forger.<TestModels.TestModel>builder()
        .withMicroOrm(new MicroOrm())
        .withMicroOrm(new MicroOrm())
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowPuttingModelGraphTwice() throws Exception {
    Forger.<TestModels.TestModel>builder()
        .withModelGraph(TestModels.MODEL_GRAPH)
        .withModelGraph(TestModels.MODEL_GRAPH)
        .build();
  }

  private static class CustomDateAdapter
      implements FakeDataGenerator<TestModels.ComplexDate>, TypeAdapter<TestModels.ComplexDate> {

    private static final long TIMESTAMP_TO_RETURN = 15;

    @Override
    public TestModels.ComplexDate generate() {
      return new TestModels.ComplexDate(TIMESTAMP_TO_RETURN);
    }

    @Override
    public TestModels.ComplexDate fromCursor(Cursor cursor, String columnName) {
      return new TestModels.ComplexDate(cursor.getLong(cursor.getColumnIndexOrThrow(columnName)));
    }

    @Override
    public void toContentValues(ContentValues contentValues, String s, TestModels.ComplexDate complexDate) {
      contentValues.put(s, complexDate.getTimestamp());
    }
  }
}
