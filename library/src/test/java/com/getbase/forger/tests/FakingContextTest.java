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

import static org.fest.assertions.api.Assertions.assertThat;

import com.getbase.forger.Forger;

import org.chalup.microorm.MicroOrm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FakingContextTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectContextOutsideOfModelGraph() throws Exception {
    mTestSubject.inContextOf(new Object());
  }

  @Test
  public void originalForgerShouldNotUseContext() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    mTestSubject.inContextOf(contact);

    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isNotEqualTo(contact.id);
  }

  @Test
  public void shouldUseTheSuppliedContextInAllRelationships() throws Exception {
    Forger<TestModels.TestModel> forgerWithContext = mTestSubject.inContextOf(TestModels.User.class).in(mContentResolver);

    TestModels.Deal deal = forgerWithContext.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    TestModels.Contact contact = forgerWithContext.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Tagging tagging = forgerWithContext.iNeed(TestModels.Tagging.class).relatedTo(contact).in(mContentResolver);
    assertThat(tagging).isNotNull();

    assertThat(contact.userId).isEqualTo(deal.userId);
    assertThat(tagging.userId).isEqualTo(deal.userId);
  }

  @Test
  public void shouldAllowOverridingCurrentContextWithAnotherObject() throws Exception {
    TestModels.Contact contactA = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contactA).isNotNull();

    TestModels.Contact contactB = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contactB).isNotNull();

    Forger<TestModels.TestModel> forgerWithContext = mTestSubject.inContextOf(contactA).inContextOf(contactB);

    TestModels.Deal deal = forgerWithContext.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    assertThat(deal.contactId).isEqualTo(contactB.id);
    assertThat(deal.contactId).isNotEqualTo(contactA.id);
  }

  @Test
  public void shouldAllowOverridingFieldsWhenDefiningContext() throws Exception {
    Forger<TestModels.TestModel> forgerWithContext = mTestSubject
        .inContextOf(TestModels.Contact.class)
        .with("id", 42)
        .in(mContentResolver);

    TestModels.Deal deal = forgerWithContext.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    assertThat(deal.contactId).isEqualTo(42);
  }
}
