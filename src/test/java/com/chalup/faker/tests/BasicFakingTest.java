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

package com.chalup.faker.tests;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.chalup.faker.Faker;
import com.chalup.microorm.MicroOrm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;
import android.content.ContentValues;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BasicFakingTest {

  Faker<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Faker<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  public static class ClassOutsideOfTheModelGraph {
  }

  @Test(expected = NullPointerException.class)
  public void shouldNotAllowFakingClassesOutsideOfModelGraph() throws Exception {
    mTestSubject.iNeed(ClassOutsideOfTheModelGraph.class).in(mContentResolver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithoutDefaultConstructor() throws Exception {
    mTestSubject.iNeed(TestModels.ClassWithoutDefaultConstructor.class).in(mContentResolver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithoutPublicDefaultConstructor() throws Exception {
    mTestSubject.iNeed(TestModels.ClassWithoutPublicDefaultConstructor.class).in(mContentResolver);
  }

  @Test
  public void shouldCreateSimpleObject() throws Exception {
    TestModels.User user = mTestSubject.iNeed(TestModels.User.class).in(mContentResolver);

    assertThat(user).isNotNull();

    assertThat(user.id).isNotEqualTo(0);
    assertThat(user.email).isNotNull();
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInOneToManyRelationship() throws Exception {
    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);

    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingParentObjectForOneToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).relatedTo(contact).in(mContentResolver);
    assertThat(deal).isNotNull();

    assertThat(deal.contactId).isEqualTo(contact.id);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectIncorrectParentObject() throws Exception {
    mTestSubject.iNeed(TestModels.Deal.class).relatedTo(new Object());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithNonBasicMemberTypes() throws Exception {
    mTestSubject.iNeed(TestModels.ClassWithNonBasicFieldType.class).in(mContentResolver);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInOneToOneRelationship() throws Exception {
    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).in(mContentResolver);

    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingParentObjectForOneToOneRelationship() throws Exception {
    TestModels.Lead lead = mTestSubject.iNeed(TestModels.Lead.class).in(mContentResolver);
    assertThat(lead).isNotNull();

    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).relatedTo(lead).in(mContentResolver);
    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(lead.id);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInManyToManyRelationship() throws Exception {
    TestModels.DealContact dealContact = mTestSubject.iNeed(TestModels.DealContact.class).in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isNotEqualTo(0);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingObjectsForManyToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    TestModels.DealContact dealContact = mTestSubject
        .iNeed(TestModels.DealContact.class)
        .relatedTo(contact)
        .relatedTo(deal)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isEqualTo(deal.id);
  }

  @Test
  public void shouldAllowSupplyingObjectsForOnlyOneSideOfManyToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.DealContact dealContact = mTestSubject
        .iNeed(TestModels.DealContact.class)
        .relatedTo(contact)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldNotAllowFakingOfObjectWithMandatoryPolymorphicRelationshipWithoutSuppliedObject() throws Exception {
    mTestSubject.iNeed(TestModels.Note.class).in(mContentResolver);
  }

  @Test
  public void shouldAllowSupplyingObjectsForPolymorphicRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Note note = mTestSubject
        .iNeed(TestModels.Note.class)
        .relatedTo(contact)
        .in(mContentResolver);

    assertThat(note).isNotNull();
    assertThat(note.notableId).isEqualTo(contact.id);
    assertThat(note.notableType).isEqualTo("Contact");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldValidateSuppliedPolymorphicObjectType() throws Exception {
    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    mTestSubject.iNeed(TestModels.Call.class).relatedTo(deal);
  }

  @Test
  public void shouldAllowFakingOfObjectWithRecursiveRelationshipWithoutSuppliedObject() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);

    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isNull();
  }

  @Test
  public void shouldAllowSupplyingObjectsForRecursiveRelationship() throws Exception {
    TestModels.Contact company = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(company).isNotNull();

    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).relatedTo(company).in(mContentResolver);
    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isEqualTo(company.id);
  }

  @Test
  public void shouldAllowOverridingFieldsInSimpleObjects() throws Exception {
    String email = "test@getbase.com";
    TestModels.User user = mTestSubject
        .iNeed(TestModels.User.class)
        .with("email", email)
        .in(mContentResolver);

    assertThat(user).isNotNull();
    assertThat(user.email).isEqualTo(email);
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfOneToManyRelationship() throws Exception {
    long contactId = 42L;
    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).with("contact_id", contactId).in(mContentResolver);

    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isEqualTo(contactId);

    verify(mContentResolver, never()).insert(eq(TestModels.CONTACT.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfOneToOneRelationship() throws Exception {
    long leadId = 42L;
    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).with("lead_id", leadId).in(mContentResolver);

    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(leadId);

    verify(mContentResolver, never()).insert(eq(TestModels.LEAD.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfManyToManyRelationship() throws Exception {
    long contactId = 42L;
    long dealId = 7L;
    TestModels.DealContact dealContact = mTestSubject.iNeed(TestModels.DealContact.class)
        .with("contact_id", contactId)
        .with("deal_id", dealId)
        .in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contactId);
    assertThat(dealContact.dealId).isEqualTo(dealId);

    verify(mContentResolver, never()).insert(eq(TestModels.CONTACT.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(eq(TestModels.DEAL.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldCreateObjectForPartiallySatisfiedDependenciesOfManyToManyRelationship() throws Exception {
    long dealId = 42L;
    TestModels.DealContact dealContact = mTestSubject.iNeed(TestModels.DealContact.class)
        .with("deal_id", dealId)
        .in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isNotEqualTo(0);
    assertThat(dealContact.dealId).isEqualTo(dealId);

    verify(mContentResolver, never()).insert(eq(TestModels.DEAL.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfPolymorphicRelationship() throws Exception {
    String notableType = "Contact";
    long notableId = 42L;

    TestModels.Note note = mTestSubject.iNeed(TestModels.Note.class)
        .with("notable_type", notableType)
        .with("notable_id", notableId)
        .in(mContentResolver);

    assertThat(note).isNotNull();
    assertThat(note.notableType).isEqualTo(notableType);
    assertThat(note.notableId).isEqualTo(notableId);

    verify(mContentResolver, never()).insert(eq(TestModels.CONTACT.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(eq(TestModels.DEAL.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(eq(TestModels.LEAD.getUri()), any(ContentValues.class));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowOverridingOnlyOneColumnInPolymorphicRelationship() throws Exception {
    mTestSubject.iNeed(TestModels.Note.class)
        .with("notable_id", 42L)
        .in(mContentResolver);
  }
}
