package org.motechproject.tasks.service;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commons.api.DataProviderLookup;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventListener;
import org.motechproject.event.listener.EventListenerRegistryService;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.tasks.domain.AdditionalData;
import org.motechproject.tasks.domain.EventParameter;
import org.motechproject.tasks.domain.Filter;
import org.motechproject.tasks.domain.Task;
import org.motechproject.tasks.domain.TaskActivity;
import org.motechproject.tasks.domain.TaskEvent;
import org.motechproject.tasks.ex.ActionNotFoundException;
import org.motechproject.tasks.ex.TaskException;
import org.motechproject.tasks.ex.TriggerNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.tasks.domain.EventParamType.DATE;
import static org.motechproject.tasks.domain.EventParamType.NUMBER;
import static org.motechproject.tasks.domain.EventParamType.TEXTAREA;
import static org.motechproject.tasks.domain.OperatorType.CONTAINS;
import static org.motechproject.tasks.domain.OperatorType.ENDSWITH;
import static org.motechproject.tasks.domain.OperatorType.EQUALS;
import static org.motechproject.tasks.domain.OperatorType.EXIST;
import static org.motechproject.tasks.domain.OperatorType.GT;
import static org.motechproject.tasks.domain.OperatorType.LT;
import static org.motechproject.tasks.domain.OperatorType.STARTSWITH;
import static org.motechproject.tasks.domain.TaskActivityType.ERROR;

public class TaskTriggerHandlerTest {
    private class TestObjectField {
        private int id = 6789;

        public int getId() {
            return id;
        }
    }

    private class TestObject {
        private TestObjectField field = new TestObjectField();

        public TestObjectField getField() {
            return field;
        }
    }

    private static final String TRIGGER_SUBJECT = "APPOINTMENT_CREATE_EVENT_SUBJECT";
    private static final String ACTION_SUBJECT = "SEND_SMS";

    private static Map<String, String> lookupFields;

    @Mock
    TaskService taskService;

    @Mock
    TaskActivityService taskActivityService;

    @Mock
    EventListenerRegistryService registryService;

    @Mock
    SettingsFacade settingsFacade;

    @Mock
    EventRelay eventRelay;

    @Mock
    DataProviderLookup dataProviderLookup;

    TestObject obj;

    TaskTriggerHandler handler;

    List<Task> tasks;
    List<TaskActivity> messages;
    Task task;
    TaskEvent triggerEvent;
    TaskEvent actionEvent;

    @BeforeClass
    public static void setLookupFields() {
        lookupFields = new HashMap<>();
        lookupFields.put("id", "123456789");
    }

    @Before
    public void setup() throws Exception {
        initTest();

        when(taskService.getAllTasks()).thenReturn(tasks);
        when(settingsFacade.getProperty("task.possible.errors")).thenReturn("5");

        obj = new TestObject();
        handler = new TaskTriggerHandler(taskService, taskActivityService, registryService, eventRelay, settingsFacade);
        handler.setDataProviders(Arrays.asList(dataProviderLookup));

        verify(taskService).getAllTasks();
        verify(registryService).registerListener(any(EventListener.class), anyString());
    }

    @Test
    public void shouldNotSendEventWhenTriggerNotFound() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenThrow(new TriggerNotFoundException(""));

        handler.handle(createEvent());

        verify(taskService).findTrigger(TRIGGER_SUBJECT);

        verify(taskService, never()).findTasksForTrigger(triggerEvent);
        verify(taskService, never()).getActionEventFor(task);
        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);
    }

    @Test
    public void shouldNotSendEventWhenActionNotFound() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenThrow(new ActionNotFoundException(""));

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertEquals("error.actionNotFound", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSentEventWhenActionHasNotSubject() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);

        actionEvent.setSubject(null);

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertEquals("error.actionWithoutSubject", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventWhenActionEventParameterHasNotValue() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);

        task.getActionInputFields().put("phone", null);

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertEquals("error.templateNull", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventIfActionEventParameterCanNotBeConvertedToNumber() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);

        task.getActionInputFields().put("phone", "1234   d");

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertEquals("error.convertToNumber", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventIfActionEventParameterCanNotBeConvertedToDate() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);

        task.getActionInputFields().put("date", "234543fgf");

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertEquals("error.convertToDate", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldDisableTaskWhenNumberPossibleErrorsIsExceeded() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);
        when(taskActivityService.errorsFromLastRun(task)).thenReturn(messages);
        task.getActionInputFields().put("message", null);

        assertTrue(task.isEnabled());

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());
        verify(taskActivityService).errorsFromLastRun(task);
        verify(taskService).save(task);
        verify(taskActivityService).addWarning(task);

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertFalse(task.isEnabled());
        assertEquals("error.templateNull", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventIfDataProvidersListIsNull() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);
        when(taskActivityService.errorsFromLastRun(task)).thenReturn(messages);

        assertTrue(task.isEnabled());

        handler.setDataProviders(null);
        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(dataProviderLookup, never()).supports(anyString());
        verify(dataProviderLookup, never()).lookup(anyString(), anyMap());
        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertFalse(task.isEnabled());
        assertEquals("error.notFoundDataProvider", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventIfDataProvidersListIsEmpty() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);
        when(taskActivityService.errorsFromLastRun(task)).thenReturn(messages);

        assertTrue(task.isEnabled());

        handler.setDataProviders(new ArrayList<DataProviderLookup>());
        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(dataProviderLookup, never()).supports(anyString());
        verify(dataProviderLookup, never()).lookup(anyString(), anyMap());
        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertFalse(task.isEnabled());
        assertEquals("error.notFoundDataProvider", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventIfDataProviderNotFoundObject() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);
        when(taskActivityService.errorsFromLastRun(task)).thenReturn(messages);

        when(dataProviderLookup.getName()).thenReturn("TEST");
        when(dataProviderLookup.supports("TestObject")).thenReturn(true);
        when(dataProviderLookup.lookup("TestObject", lookupFields)).thenReturn(null);

        assertTrue(task.isEnabled());

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(dataProviderLookup).supports("TestObject");
        verify(dataProviderLookup).lookup("TestObject", lookupFields);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertFalse(task.isEnabled());
        assertEquals("error.notFoundObjectForType", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventIfDataProviderObjectNotContainsField() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);
        when(taskActivityService.errorsFromLastRun(task)).thenReturn(messages);

        when(dataProviderLookup.getName()).thenReturn("TEST");
        when(dataProviderLookup.supports("TestObject")).thenReturn(true);
        when(dataProviderLookup.lookup("TestObject", lookupFields)).thenReturn(new Object());

        assertTrue(task.isEnabled());

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(dataProviderLookup).supports("TestObject");
        verify(dataProviderLookup).lookup("TestObject", lookupFields);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertFalse(task.isEnabled());
        assertEquals("error.objectNotContainsField", captor.getValue().getMessageKey());
    }

    @Test
    public void shouldNotSendEventWhenTaskIsDisabled() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);

        task.setEnabled(false);

        handler.handle(createEvent());

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService, never()).getActionEventFor(task);
        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);
    }

    @Test
    public void shouldSendEventForGivenTrigger() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);

        when(dataProviderLookup.getName()).thenReturn("TEST");
        when(dataProviderLookup.supports("TestObject")).thenReturn(true);
        when(dataProviderLookup.lookup("TestObject", lookupFields)).thenReturn(obj);

        ArgumentCaptor<MotechEvent> captor = ArgumentCaptor.forClass(MotechEvent.class);

        handler.handle(createEvent());

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(eventRelay).sendEventMessage(captor.capture());
        verify(taskActivityService).addSuccess(task);

        MotechEvent motechEvent = captor.getValue();

        assertNotNull(motechEvent);
        assertNotNull(motechEvent.getSubject());
        assertNotNull(motechEvent.getParameters());

        assertEquals(5, motechEvent.getParameters().size());
        assertEquals(ACTION_SUBJECT, motechEvent.getSubject());
        assertEquals(task.getActionInputFields().get("phone"), motechEvent.getParameters().get("phone").toString());
        assertEquals("Hello 123456789, You have an appointment on 2012-11-20", motechEvent.getParameters().get("message"));
        assertEquals("string: Event-Name, date: 20121120", motechEvent.getParameters().get("manipulation"));
        assertEquals(DateTime.parse(task.getActionInputFields().get("date"), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm Z")), motechEvent.getParameters().get("date"));
        assertEquals("test: 6789", motechEvent.getParameters().get("ds"));
    }

    @Test
    public void shouldPassFiltersCriteria() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);

        when(dataProviderLookup.getName()).thenReturn("TEST");
        when(dataProviderLookup.supports("TestObject")).thenReturn(true);
        when(dataProviderLookup.lookup("TestObject", lookupFields)).thenReturn(obj);

        addFilters();
        ArgumentCaptor<MotechEvent> captor = ArgumentCaptor.forClass(MotechEvent.class);

        handler.handle(createEvent());

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(eventRelay).sendEventMessage(captor.capture());
        verify(taskActivityService).addSuccess(task);
    }

    @Test
    public void shouldNotSendEventIfDateFormatInManipulationIsNotValid() throws Exception {
        when(taskService.findTrigger(TRIGGER_SUBJECT)).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(tasks);
        when(taskService.getActionEventFor(task)).thenReturn(actionEvent);

        task.getActionInputFields().put("message", "{{trigger.startDate?dateTime(BadFormat)}}");

        handler.handle(createEvent());
        ArgumentCaptor<TaskException> captor = ArgumentCaptor.forClass(TaskException.class);

        verify(taskService).findTrigger(TRIGGER_SUBJECT);
        verify(taskService).findTasksForTrigger(triggerEvent);
        verify(taskService).getActionEventFor(task);
        verify(taskActivityService).addError(eq(task), captor.capture());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(taskActivityService, never()).addSuccess(task);

        assertEquals("error.date.format", captor.getValue().getMessageKey());
    }

    private MotechEvent createEvent() {
        Map<String, Object> param = new HashMap<>(4);
        param.put("externalId", 123456789);
        param.put("startDate", new LocalDate(2012, 11, 20));
        param.put("endDate", new LocalDate(2012, 11, 29));
        param.put("facilityId", 987654321);
        param.put("eventName", "event name");

        return new MotechEvent(TRIGGER_SUBJECT, param);
    }

    private void initTest() throws Exception {
        initMocks(this);

        tasks = new ArrayList<>();

        String trigger = String.format("Appointments:appointments-bundle:0.15:%s", TRIGGER_SUBJECT);
        String action = String.format("SMS:sms-bundle:0.15:%s", ACTION_SUBJECT);

        Map<String, String> actionInputFields = new HashMap<>();
        actionInputFields.put("phone", "123456");
        actionInputFields.put("message", "Hello {{trigger.externalId}}, You have an appointment on {{trigger.startDate}}");
        actionInputFields.put("manipulation", "string: {{trigger.eventName?toUpper?toLower?capitalize?join(-)}}, date: {{trigger.startDate?dateTime(yyyyMMdd)}}");
        actionInputFields.put("date", "2012-12-21 21:21 +0100");
        actionInputFields.put("ds", "test: {{ad.TEST.TestObject#1.field.id}}");

        task = new Task(trigger, action, actionInputFields);
        task.setId("taskId1");
        task.setFilters(new ArrayList<Filter>());
        tasks.add(task);

        Map<String, List<AdditionalData>> additionalData = new HashMap<>(1);
        additionalData.put("TEST", Arrays.asList(new AdditionalData(1L, "TestObject", "id", "externalId")));

        task.setAdditionalData(additionalData);

        List<EventParameter> triggerEventParameters = new ArrayList<>();
        triggerEventParameters.add(new EventParameter("ExternalID", "externalId"));
        triggerEventParameters.add(new EventParameter("StartDate", "startDate", DATE));
        triggerEventParameters.add(new EventParameter("EndDate", "endDate", DATE));
        triggerEventParameters.add(new EventParameter("FacilityId", "facilityId"));
        triggerEventParameters.add(new EventParameter("EventName", "eventName"));

        triggerEvent = new TaskEvent();
        triggerEvent.setSubject(TRIGGER_SUBJECT);
        triggerEvent.setEventParameters(triggerEventParameters);

        List<EventParameter> actionEventParameters = new ArrayList<>();
        actionEventParameters.add(new EventParameter("Phone", "phone", NUMBER));
        actionEventParameters.add(new EventParameter("Message", "message", TEXTAREA));
        actionEventParameters.add(new EventParameter("Date", "date", DATE));
        actionEventParameters.add(new EventParameter("Manipulation", "manipulation"));
        actionEventParameters.add(new EventParameter("DS", "ds"));

        actionEvent = new TaskEvent();
        actionEvent.setSubject(ACTION_SUBJECT);
        actionEvent.setEventParameters(actionEventParameters);
        actionEvent.setDisplayName("SMS");

        messages = new ArrayList<>();
        messages.add(new TaskActivity("Error1", task.getId(), ERROR));
        messages.add(new TaskActivity("Error2", task.getId(), ERROR));
        messages.add(new TaskActivity("Error3", task.getId(), ERROR));
        messages.add(new TaskActivity("Error4", task.getId(), ERROR));
        messages.add(new TaskActivity("Error5", task.getId(), ERROR));
    }

    private void addFilters() {
        List<Filter> filters = new ArrayList<>();
        filters.add(new Filter(new EventParameter("EventName", "eventName"), true, CONTAINS.getValue(), "ven"));
        filters.add(new Filter(new EventParameter("EventName", "eventName"), true, EXIST.getValue(), ""));
        filters.add(new Filter(new EventParameter("EventName", "eventName"), true, EQUALS.getValue(), "event name"));
        filters.add(new Filter(new EventParameter("EventName", "eventName"), true, STARTSWITH.getValue(), "ev"));
        filters.add(new Filter(new EventParameter("EventName", "eventName"), true, ENDSWITH.getValue(), "me"));
        filters.add(new Filter(new EventParameter("ExternalID", "externalId", NUMBER), true, GT.getValue(), "19"));
        filters.add(new Filter(new EventParameter("ExternalID", "externalId", NUMBER), true, LT.getValue(), "1234567891"));
        filters.add(new Filter(new EventParameter("ExternalID", "externalId", NUMBER), true, EQUALS.getValue(), "123456789"));
        filters.add(new Filter(new EventParameter("ExternalID", "externalId", NUMBER), true, EXIST.getValue(), ""));
        filters.add(new Filter(new EventParameter("ExternalID", "externalId", NUMBER), false, GT.getValue(), "1234567891"));

        task.setFilters(filters);
    }
}
