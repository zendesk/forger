/*
 * Copyright (C) 2013 Jerzy Chalupski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chalup.forger;

interface FakeDataGenerators {
  public static class StringGenerator implements FakeDataGenerator<String> {

    private long mCount = 1;

    @Override
    public String generate() {
      return "Test" + String.valueOf(mCount++);
    }
  }

  public static class ShortGenerator implements FakeDataGenerator<Short> {
    private short mCount = 1;

    @Override
    public Short generate() {
      return mCount++;
    }
  }

  public static class IntegerGenerator implements FakeDataGenerator<Integer> {
    private int mCount = 1;

    @Override
    public Integer generate() {
      return mCount++;
    }
  }

  public static class LongGenerator implements FakeDataGenerator<Long> {
    private long mCount = 1;

    @Override
    public Long generate() {
      return mCount++;
    }
  }

  public static class BooleanGenerator implements FakeDataGenerator<Boolean> {
    @Override
    public Boolean generate() {
      return false;
    }
  }

  public static class FloatGenerator implements FakeDataGenerator<Float> {
    private float mCount = 1;

    @Override
    public Float generate() {
      return mCount++;
    }
  }

  public static class DoubleGenerator implements FakeDataGenerator<Double> {
    private double mCount = 1;

    @Override
    public Double generate() {
      return mCount++;
    }
  }
}
