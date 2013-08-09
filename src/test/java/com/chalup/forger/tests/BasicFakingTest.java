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

package com.chalup.forger.tests;

import static com.chalup.forger.tests.TestModels.CONTACT;
import static com.chalup.forger.tests.TestModels.DEAL;
import static com.chalup.forger.tests.TestModels.LEAD;
import static com.chalup.forger.tests.TestModels.MODEL_GRAPH;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.chalup.forger.Forger;
import com.chalup.forger.tests.TestModels.Call;
import com.chalup.forger.tests.TestModels.ClassWithNonBasicFieldType;
import com.chalup.forger.tests.TestModels.ClassWithoutDefaultConstructor;
import com.chalup.forger.tests.TestModels.ClassWithoutPublicDefaultConstructor;
import com.chalup.forger.tests.TestModels.Contact;
import com.chalup.forger.tests.TestModels.ContactData;
import com.chalup.forger.tests.TestModels.Deal;
import com.chalup.forger.tests.TestModels.DealContact;
import com.chalup.forger.tests.TestModels.Lead;
import com.chalup.forger.tests.TestModels.Note;
import com.chalup.forger.tests.TestModels.Tagging;
import com.chalup.forger.tests.TestModels.TestModel;
import com.chalup.forger.tests.TestModels.User;
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

  Forger<TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<TestModel>(MODEL_GRAPH, new MicroOrm());
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
    mTestSubject.iNeed(ClassWithoutDefaultConstructor.class).in(mContentResolver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithoutPublicDefaultConstructor() throws Exception {
    mTestSubject.iNeed(ClassWithoutPublicDefaultConstructor.class).in(mContentResolver);
  }

  @Test
  public void shouldCreateSimpleObject() throws Exception {
    User user = mTestSubject.iNeed(User.class).in(mContentResolver);

    assertThat(user).isNotNull();

    assertThat(user.id).isNotEqualTo(0);
    assertThat(user.email).isNotNull();
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInOneToManyRelationship() throws Exception {
    Deal deal = mTestSubject.iNeed(Deal.class).in(mContentResolver);

    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingParentObjectForOneToManyRelationship() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Deal deal = mTestSubject.iNeed(Deal.class).relatedTo(contact).in(mContentResolver);
    assertThat(deal).isNotNull();

    assertThat(deal.contactId).isEqualTo(contact.id);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectIncorrectParentObject() throws Exception {
    mTestSubject.iNeed(Deal.class).relatedTo(new Object());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithNonBasicMemberTypes() throws Exception {
    mTestSubject.iNeed(ClassWithNonBasicFieldType.class).in(mContentResolver);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInOneToOneRelationship() throws Exception {
    ContactData contactData = mTestSubject.iNeed(ContactData.class).in(mContentResolver);

    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingParentObjectForOneToOneRelationship() throws Exception {
    Lead lead = mTestSubject.iNeed(Lead.class).in(mContentResolver);
    assertThat(lead).isNotNull();

    ContactData contactData = mTestSubject.iNeed(ContactData.class).relatedTo(lead).in(mContentResolver);
    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(lead.id);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInManyToManyRelationship() throws Exception {
    DealContact dealContact = mTestSubject.iNeed(DealContact.class).in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isNotEqualTo(0);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingObjectsForManyToManyRelationship() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Deal deal = mTestSubject.iNeed(Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    DealContact dealContact = mTestSubject
        .iNeed(DealContact.class)
        .relatedTo(contact)
        .relatedTo(deal)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isEqualTo(deal.id);
  }

  @Test
  public void shouldAllowSupplyingObjectsForOnlyOneSideOfManyToManyRelationship() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    DealContact dealContact = mTestSubject
        .iNeed(DealContact.class)
        .relatedTo(contact)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldNotAllowFakingOfObjectWithMandatoryPolymorphicRelationshipWithoutSuppliedObject() throws Exception {
    mTestSubject.iNeed(Note.class).in(mContentResolver);
  }

  @Test
  public void shouldAllowSupplyingObjectsForPolymorphicRelationship() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Note note = mTestSubject
        .iNeed(Note.class)
        .relatedTo(contact)
        .in(mContentResolver);

    assertThat(note).isNotNull();
    assertThat(note.notableId).isEqualTo(contact.id);
    assertThat(note.notableType).isEqualTo("Contact");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldValidateSuppliedPolymorphicObjectType() throws Exception {
    Deal deal = mTestSubject.iNeed(Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    mTestSubject.iNeed(Call.class).relatedTo(deal);
  }

  @Test
  public void shouldAllowFakingOfObjectWithRecursiveRelationshipWithoutSuppliedObject() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);

    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isNull();
  }

  @Test
  public void shouldAllowSupplyingObjectsForRecursiveRelationship() throws Exception {
    Contact company = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(company).isNotNull();

    Contact contact = mTestSubject.iNeed(Contact.class).relatedTo(company).in(mContentResolver);
    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isEqualTo(company.id);
  }

  @Test
  public void shouldAllowOverridingFieldsInSimpleObjects() throws Exception {
    String email = "test@getbase.com";
    User user = mTestSubject
        .iNeed(User.class)
        .with("email", email)
        .in(mContentResolver);

    assertThat(user).isNotNull();
    assertThat(user.email).isEqualTo(email);
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfOneToManyRelationship() throws Exception {
    long contactId = 42L;
    Deal deal = mTestSubject.iNeed(Deal.class).with("contact_id", contactId).in(mContentResolver);

    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isEqualTo(contactId);

    verify(mContentResolver, never()).insert(eq(CONTACT.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfOneToOneRelationship() throws Exception {
    long leadId = 42L;
    ContactData contactData = mTestSubject.iNeed(ContactData.class).with("lead_id", leadId).in(mContentResolver);

    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(leadId);

    verify(mContentResolver, never()).insert(eq(LEAD.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfManyToManyRelationship() throws Exception {
    long contactId = 42L;
    long dealId = 7L;
    DealContact dealContact = mTestSubject.iNeed(DealContact.class)
        .with("contact_id", contactId)
        .with("deal_id", dealId)
        .in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contactId);
    assertThat(dealContact.dealId).isEqualTo(dealId);

    verify(mContentResolver, never()).insert(eq(CONTACT.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(eq(DEAL.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldCreateObjectForPartiallySatisfiedDependenciesOfManyToManyRelationship() throws Exception {
    long dealId = 42L;
    DealContact dealContact = mTestSubject.iNeed(DealContact.class)
        .with("deal_id", dealId)
        .in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isNotEqualTo(0);
    assertThat(dealContact.dealId).isEqualTo(dealId);

    verify(mContentResolver, never()).insert(eq(DEAL.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfPolymorphicRelationship() throws Exception {
    String notableType = "Contact";
    long notableId = 42L;

    Note note = mTestSubject.iNeed(Note.class)
        .with("notable_type", notableType)
        .with("notable_id", notableId)
        .in(mContentResolver);

    assertThat(note).isNotNull();
    assertThat(note.notableType).isEqualTo(notableType);
    assertThat(note.notableId).isEqualTo(notableId);

    verify(mContentResolver, never()).insert(eq(CONTACT.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(eq(DEAL.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(eq(LEAD.getUri()), any(ContentValues.class));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowOverridingOnlyOneColumnInPolymorphicRelationship() throws Exception {
    mTestSubject.iNeed(Note.class)
        .with("notable_id", 42L)
        .in(mContentResolver);
  }

  @Test
  public void shouldCreateObjectWhenOneSideOfManyToManyRelationshipIsPolymorphic() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Tagging tagging = mTestSubject.iNeed(Tagging.class).relatedTo(contact).in(mContentResolver);
    assertThat(tagging).isNotNull();
    assertThat(tagging.taggableType).isEqualTo("Contact");
    assertThat(tagging.taggableId).isEqualTo(contact.id);
    assertThat(tagging.userId).isNotEqualTo(0);
    assertThat(tagging.tagId).isNotEqualTo(0);
  }

  @Test
  public void shouldSatisfyOneToManyDependenciesFromSuppliedContext() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Forger<TestModel> forgerWithContext = mTestSubject.inContextOf(contact);

    Deal deal = forgerWithContext.iNeed(Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isEqualTo(contact.id);
  }

  @Test
  public void shouldSatisfyOneToOneDependenciesFromSuppliedContext() throws Exception {
    Lead lead = mTestSubject.iNeed(Lead.class).in(mContentResolver);
    assertThat(lead).isNotNull();

    Forger<TestModel> forgerWithContext = mTestSubject.inContextOf(lead);

    ContactData contactData = forgerWithContext.iNeed(ContactData.class).in(mContentResolver);
    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(lead.id);
  }

  @Test
  public void shouldSatisfyRecursiveDependenciesFromSuppliedContext() throws Exception {
    Contact company = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(company).isNotNull();

    Forger<TestModel> forgerWithContext = mTestSubject.inContextOf(company);

    Contact contact = forgerWithContext.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isEqualTo(company.id);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldNotSatisfyPolymorphicDependenciesFromSuppliedContext() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Forger<TestModel> forgerWithContext = mTestSubject.inContextOf(contact);

    forgerWithContext.iNeed(Note.class).in(mContentResolver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectContextOutsideOfModelGraph() throws Exception {
    mTestSubject.inContextOf(new Object());
  }

  @Test
  public void originalForgerShouldNotUseContext() throws Exception {
    Contact contact = mTestSubject.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    mTestSubject.inContextOf(contact);

    Deal deal = mTestSubject.iNeed(Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isNotEqualTo(contact.id);
  }

  @Test
  public void shouldUseTheSuppliedContextInAllRelationships() throws Exception {
    Forger<TestModel> forgerWithContext = mTestSubject.inContextOf(User.class).in(mContentResolver);

    Deal deal = forgerWithContext.iNeed(Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    Contact contact = forgerWithContext.iNeed(Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Tagging tagging = forgerWithContext.iNeed(Tagging.class).relatedTo(contact).in(mContentResolver);
    assertThat(tagging).isNotNull();

    assertThat(contact.userId).isEqualTo(deal.userId);
    assertThat(tagging.userId).isEqualTo(deal.userId);
  }
}
