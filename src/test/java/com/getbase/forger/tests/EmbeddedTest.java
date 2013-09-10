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

package com.getbase.forger.tests;

import static org.fest.assertions.Assertions.assertThat;

import com.getbase.forger.Forger;
import com.getbase.forger.tests.TestModels.ExtendedPersonalInfo;
import com.getbase.forger.tests.TestModels.PersonalInfo;
import com.getbase.forger.tests.TestModels.PersonalInfoV2;
import com.getbase.forger.tests.TestModels.PersonalInfoV3;

import org.chalup.microorm.MicroOrm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EmbeddedTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test
  public void shouldBeAbleToFakeEmbeddedObjects() throws Exception {
    PersonalInfo personalInfo = mTestSubject.iNeed(PersonalInfo.class).in(mContentResolver);

    assertThat(personalInfo).isNotNull();

    assertThat(personalInfo.name).isNotNull();
    assertThat(personalInfo.socialInformation.facebook).isNotNull();
  }

  @Test
  public void shouldBeAbleToFakeEmbeddedObjectsInSuperclass() throws Exception {
    ExtendedPersonalInfo contactInfo = mTestSubject.iNeed(ExtendedPersonalInfo.class).in(mContentResolver);

    assertThat(contactInfo).isNotNull();

    assertThat(contactInfo.name).isNotNull();
    assertThat(contactInfo.surname).isNotNull();
    assertThat(contactInfo.socialInformation.facebook).isNotNull();
  }

  @Test
  public void shouldBeAbleToFakeEmbeddedObjectsWithColumnsInSuperclass() throws Exception {
    PersonalInfoV2 contactInfo = mTestSubject.iNeed(PersonalInfoV2.class).in(mContentResolver);

    assertThat(contactInfo).isNotNull();

    assertThat(contactInfo.name).isNotNull();
    assertThat(contactInfo.socialInformation.facebook).isNotNull();
    assertThat(contactInfo.socialInformation.linkedin).isNotNull();
  }

  @Test
  public void shouldBeAbleToFakeNestedEmbeddedObjects() throws Exception {
    PersonalInfoV3 personalInfo = mTestSubject.iNeed(PersonalInfoV3.class).in(mContentResolver);

    assertThat(personalInfo).isNotNull();

    assertThat(personalInfo.name).isNotNull();
    assertThat(personalInfo.socialInformation.facebook).isNotNull();
    assertThat(personalInfo.contactInfo.email).isNotNull();
    assertThat(personalInfo.contactInfo.phoneNumber.countryCode).isNotNull();
    assertThat(personalInfo.contactInfo.phoneNumber.extension).isNotNull();
    assertThat(personalInfo.contactInfo.phoneNumber.nationalNumber).isNotNull();
  }
}
