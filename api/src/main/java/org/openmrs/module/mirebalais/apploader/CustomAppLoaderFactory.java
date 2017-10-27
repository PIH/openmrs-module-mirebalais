package org.openmrs.module.mirebalais.apploader;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.module.appframework.domain.AppTemplate;
import org.openmrs.module.appframework.domain.Extension;
import org.openmrs.module.appframework.factory.AppFrameworkFactory;
import org.openmrs.module.appframework.feature.FeatureToggleProperties;
import org.openmrs.module.coreapps.CoreAppsConstants;
import org.openmrs.module.mirebalais.apploader.apps.PatientRegistrationApp;
import org.openmrs.module.mirebalaisreports.MirebalaisReportsProperties;
import org.openmrs.module.mirebalaisreports.definitions.BaseReportManager;
import org.openmrs.module.mirebalaisreports.definitions.FullDataExportBuilder;
import org.openmrs.module.pihcore.PihCoreConstants;
import org.openmrs.module.pihcore.config.Components;
import org.openmrs.module.pihcore.config.Config;
import org.openmrs.module.pihcore.config.ConfigDescriptor;
import org.openmrs.module.pihcore.deploy.bundle.core.EncounterRoleBundle;
import org.openmrs.module.pihcore.deploy.bundle.core.RelationshipTypeBundle;
import org.openmrs.module.pihcore.metadata.core.EncounterTypes;
import org.openmrs.module.pihcore.metadata.core.LocationTags;
import org.openmrs.module.pihcore.metadata.core.Privileges;
import org.openmrs.module.pihcore.metadata.haiti.mirebalais.PihHaitiPrograms;
import org.openmrs.ui.framework.WebConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderConstants.Apps;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderConstants.EncounterTemplates;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderConstants.ExtensionPoints;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderConstants.Extensions;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addFeatureToggleToApp;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addFeatureToggleToExtension;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToClinicianDashboardFirstColumn;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToClinicianDashboardSecondColumn;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToHivDashboardFirstColumn;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToHivDashboardSecondColumn;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToHivSummaryDashboardFirstColumn;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToHomePage;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToHomePageWithoutUsingRouter;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToProgramSummaryListPage;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToRegistrationSummaryContent;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToRegistrationSummarySecondColumnContent;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToSystemAdministrationPage;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToZikaDashboardFirstColumn;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.addToZikaDashboardSecondColumn;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.andCreateVisit;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.app;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.arrayNode;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.awaitingAdmissionAction;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.containsExtension;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.dailyReport;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.dashboardTab;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.dataExport;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.determineHtmlFormPath;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.editSimpleHtmlFormLink;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.encounterTemplate;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.enterSimpleHtmlFormLink;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.enterStandardHtmlFormLink;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.extension;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.findPatientTemplateApp;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.fragmentExtension;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.header;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.map;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.monitoringReport;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.objectNode;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.overallAction;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.overallRegistrationAction;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.overviewReport;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.registerTemplateForEncounterType;
import static org.openmrs.module.mirebalais.apploader.CustomAppLoaderUtil.visitAction;
import static org.openmrs.module.mirebalais.require.RequireUtil.and;
import static org.openmrs.module.mirebalais.require.RequireUtil.or;
import static org.openmrs.module.mirebalais.require.RequireUtil.patientAgeUnknown;
import static org.openmrs.module.mirebalais.require.RequireUtil.patientDoesNotActiveVisit;
import static org.openmrs.module.mirebalais.require.RequireUtil.patientHasActiveVisit;
import static org.openmrs.module.mirebalais.require.RequireUtil.patientIsAdult;
import static org.openmrs.module.mirebalais.require.RequireUtil.patientIsChild;
import static org.openmrs.module.mirebalais.require.RequireUtil.patientNotDead;
import static org.openmrs.module.mirebalais.require.RequireUtil.patientVisitWithinPastThirtyDays;
import static org.openmrs.module.mirebalais.require.RequireUtil.sessionLocationHasTag;
import static org.openmrs.module.mirebalais.require.RequireUtil.userHasPrivilege;
import static org.openmrs.module.mirebalaisreports.definitions.BaseReportManager.REPORTING_DATA_EXPORT_REPORTS_ORDER;

@Component("customAppLoaderFactory")
public class CustomAppLoaderFactory implements AppFrameworkFactory {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private Config config;

    @Autowired
    private FeatureToggleProperties featureToggles;

    @Autowired
    private PatientRegistrationApp patientRegistrationApp;

    @Autowired
    private FullDataExportBuilder fullDataExportBuilder;

    private List<AppDescriptor> apps = new ArrayList<AppDescriptor>();

    private List<Extension> extensions = new ArrayList<Extension>();

    private Boolean readyForRefresh = false;

    private String patientVisitsPageUrl = "";

    private String patientVisitsPageWithSpecificVisitUrl = "";

    @Override
    public List<AppDescriptor> getAppDescriptors() throws IOException {
        if (readyForRefresh) {
            loadAppsAndExtensions();
        }
        return apps;
    }

    @Override
    public List<Extension> getExtensions() throws IOException {
        if (readyForRefresh) {
            loadAppsAndExtensions();
        }
        return extensions;
    }

    @Override
    public List<AppTemplate> getAppTemplates() throws IOException {
        return null;
    }


    private void loadAppsAndExtensions() {

        configureHeader(config);
        setupDefaultEncounterTemplates();

        // determine whether we are using the new visit note
        if (config.isComponentEnabled(Components.VISIT_NOTE)) {
            patientVisitsPageUrl = "/pihcore/visit/visit.page?patient={{patient.uuid}}#/visitList";
            patientVisitsPageWithSpecificVisitUrl = "/pihcore/visit/visit.page?patient={{patient.uuid}}&visit={{visit.uuid}}#/overview";
        }
        else {
            patientVisitsPageUrl = "/coreapps/patientdashboard/patientDashboard.page?patientId={{patient.patientId}}";
            patientVisitsPageWithSpecificVisitUrl = patientVisitsPageUrl + "&visitId={{visit.visitId}}";
        }

        if (config.isComponentEnabled(Components.VISIT_MANAGEMENT)) {
            enableVisitManagement();
        }

        if (config.isComponentEnabled(Components.ACTIVE_VISITS)) {
            enableActiveVisits();
        }

        if (config.isComponentEnabled(Components.CHECK_IN)) {
            enableCheckIn(config);
        }

        if (config.isComponentEnabled(Components.UHM_VITALS) ||
                config.isComponentEnabled(Components.VITALS) ) {
            enableVitals();
        }

        if (config.isComponentEnabled(Components.CONSULT)) {
            enableConsult();
        }

        if (config.isComponentEnabled(Components.ED_CONSULT)) {
            enableEDConsult();
        }

        if (config.isComponentEnabled(Components.ADT)) {
            enableADT();
        }

        if (config.isComponentEnabled(Components.DEATH_CERTIFICATE)) {
            enableDeathCertificate();
        }

        if (config.isComponentEnabled(Components.RADIOLOGY)) {
            enableRadiology();
        }

        if (config.isComponentEnabled(Components.DISPENSING)) {
            enableDispensing();
        }

        if (config.isComponentEnabled(Components.SURGERY)) {
            enableSurgery();
        }

        if (config.isComponentEnabled(Components.ONCOLOGY)) {
            enableOncology();
        }

        if (config.isComponentEnabled(Components.LAB_RESULTS)) {
            enableLabResults();
        }

        if (config.isComponentEnabled(Components.NCD)) {
            enableNCDs();
        }

        if (config.isComponentEnabled(Components.MENTAL_HEALTH)) {
            enableMentalHealth();
        }

        if (config.isComponentEnabled(Components.OVERVIEW_REPORTS)) {
            enableOverviewReports();
        }

        if (config.isComponentEnabled(Components.MONITORING_REPORTS)) {
            enableMonitoringReports();
        }

        if (config.isComponentEnabled(Components.DATA_EXPORTS)) {
            enableDataExports();
        }

        if (config.isComponentEnabled(Components.ARCHIVES)) {
            enableArchives();
        }

        if (config.isComponentEnabled(Components.WRISTBANDS)) {
            enableWristbands();
        }

        if (config.isComponentEnabled(Components.APPOINTMENT_SCHEDULING)) {
            enableAppointmentScheduling();
        }

        if (config.isComponentEnabled(Components.SYSTEM_ADMINISTRATION)) {
            enableSystemAdministration();
        }

        if (config.isComponentEnabled(Components.MANAGE_PRINTERS)) {
            enableManagePrinters();
        }

        if (config.isComponentEnabled(Components.MY_ACCOUNT)) {
            enableMyAccount();
        }

        if (config.isComponentEnabled(Components.PATIENT_REGISTRATION)) {
            enablePatientRegistration();
        }

        if (config.isComponentEnabled(Components.LEGACY_MPI)) {
            enableLegacyMPI();
        }

        if (config.isComponentEnabled(Components.LACOLLINE_PATIENT_REGISTRATION_ENCOUNTER_TYPES)) {
            registerLacollinePatientRegistrationEncounterTypes();
        }

        if (config.isComponentEnabled(Components.CLINICIAN_DASHBOARD)) {
            enableClinicianDashboard();
        }

        if (config.isComponentEnabled(Components.ALLERGIES)) {
            enableAllergies();
        }

        // will need to add chart search module back to distro if we want to use this again
        if (config.isComponentEnabled(Components.CHART_SEARCH)) {
            enableChartSearch();
        }

        if (config.isComponentEnabled(Components.WAITING_FOR_CONSULT)) {
            enableWaitingForConsult();
        }

        if (config.isComponentEnabled(Components.PRIMARY_CARE)) {
            enablePrimaryCare();
        }

        if (config.isComponentEnabled(Components.ED_TRIAGE)) {
            enableEDTriage();
        }

        if (config.isComponentEnabled(Components.ED_TRIAGE_QUEUE)) {
            enableEDTriageQueue();
        }

        if (config.isComponentEnabled(Components.CHW_APP)) {
            enableCHWApp();
        }

        if (config.isComponentEnabled(Components.BIOMETRICS_FINGERPRINTS)) {
            enableBiometrics(config);
        }

        if (config.isComponentEnabled(Components.TODAYS_VISITS)) {
            enableTodaysVisits();
        }

        if (config.isComponentEnabled(Components.LAB_TRACKING)) {
            enableLabTracking();
        }

        if (config.isComponentEnabled(Components.PROGRAMS)) {
            enablePrograms(config, featureToggles);
        }

        if (config.isComponentEnabled(Components.RELATIONSHIPS)) {
            enableRelationships();
        }

        if (config.isComponentEnabled(Components.EXPORT_PATIENTS)) {
            enableExportPatients();
        }

        if (config.isComponentEnabled(Components.IMPORT_PATIENTS)) {
            enableImportPatients();
        }

        if (config.isComponentEnabled(Components.PATIENT_DOCUMENTS)) {
            enablePatientDocuments();
        }

        readyForRefresh = false;
    }

    private void configureHeader(Config config){
        if (config.getCountry().equals(ConfigDescriptor.Country.HAITI)) {
            extensions.add(header(Extensions.PIH_HEADER_EXTENSION, "/ms/uiframework/resource/mirebalais/images/partners_in_health_logo.png"));
        } else if (config.getCountry().equals(ConfigDescriptor.Country.LIBERIA) || (config.getCountry().equals(ConfigDescriptor.Country.SIERRA_LEONE))) {
            extensions.add(header(Extensions.PIH_HEADER_EXTENSION, "/ms/uiframework/resource/mirebalais/images/partners_in_health_logo_with_english_name.png"));
        }

    }

    // TODO will these be needed/used after we switch to the visit note view?
    private void setupDefaultEncounterTemplates() {

        extensions.add(encounterTemplate(CustomAppLoaderConstants.EncounterTemplates.DEFAULT,
                "coreapps",
                "patientdashboard/encountertemplate/defaultEncounterTemplate"));

        extensions.add(encounterTemplate(EncounterTemplates.NO_DETAILS,
                "coreapps",
                "patientdashboard/encountertemplate/noDetailsEncounterTemplate"));

        extensions.add(encounterTemplate(EncounterTemplates.ED_TRIAGE,
                "edtriageapp",
                "edtriageEncounterTemplate"));

    }

    // TODO does this need to be modified for the new visit note at all?
    private void enableVisitManagement() {

        extensions.add(overallAction(Extensions.CREATE_VISIT_OVERALL_ACTION,
                "coreapps.task.startVisit.label",
                "icon-check-in",
                "script",
                "visit.showQuickVisitCreationDialog({{patient.patientId}})",
                "Task: coreapps.createVisit",
                and(patientDoesNotActiveVisit(), patientNotDead())));

        extensions.add(overallAction(Extensions.CREATE_RETROSPECTIVE_VISIT_OVERALL_ACTION,
                "coreapps.task.createRetrospectiveVisit.label",
                "icon-plus",
                "script",
                "visit.showRetrospectiveVisitCreationDialog()",
                "Task: coreapps.createRetrospectiveVisit",
                null));

        extensions.add(overallAction(Extensions.MERGE_VISITS_OVERALL_ACTION,
                "coreapps.task.mergeVisits.label",
                "icon-link",
                "link",
                "coreapps/mergeVisits.page?patientId={{patient.uuid}}",
                "Task: coreapps.mergeVisits",
                null));

        // this provides the javascript & dialogs the backs the overall action buttons (to start/end visits, etc)
        extensions.add(fragmentExtension(Extensions.VISIT_ACTIONS_INCLUDES,
                "coreapps",
                "patientdashboard/visitIncludes",
                null,
                ExtensionPoints.DASHBOARD_INCLUDE_FRAGMENTS,
                map("patientVisitsPage", patientVisitsPageWithSpecificVisitUrl)));

    }

    private void enableActiveVisits() {

        apps.add(addToHomePage(app(Apps.ACTIVE_VISITS,
                "coreapps.activeVisits.app.label",
                "icon-check-in",
                "coreapps/activeVisits.page?app=" + Apps.ACTIVE_VISITS,
                "App: coreapps.activeVisits",
                objectNode("patientPageUrl", patientVisitsPageWithSpecificVisitUrl))));

        addFeatureToggleToApp(findAppById(Apps.ACTIVE_VISITS), "oldActiveVisits");


        apps.add(addToHomePage(app(Apps.ACTIVE_VISITS_LIST,
                "coreapps.activeVisits.app.label",
                "icon-check-in",
                "pihcore/reports/activeVisitsList.page?app=" + Apps.ACTIVE_VISITS,
                "App: coreapps.activeVisits",
                objectNode("patientPageUrl", patientVisitsPageWithSpecificVisitUrl))));

        addFeatureToggleToApp(findAppById(Apps.ACTIVE_VISITS_LIST), "newActiveVisits");

    }

    private void enableCheckIn(Config config) {

        // currently, this app is hard-coded to the default check-in form and requires archives room (?)
        if (config.isComponentEnabled(Components.CHECK_IN_HOMEPAGE_APP)) {
            apps.add(addToHomePage(findPatientTemplateApp(Apps.CHECK_IN,
                            "mirebalais.app.patientRegistration.checkin.label",
                            "icon-paste",
                            "App: mirebalais.checkin",
                    "/pihcore/checkin/checkin.page?patientId={{patientId}}",
                       //     "/registrationapp/registrationSummary.page?patientId={{patientId}}&breadcrumbOverrideProvider=coreapps&breadcrumbOverridePage=findpatient%2FfindPatient&breadcrumbOverrideApp=" + Apps.CHECK_IN + "&breadcrumbOverrideLabel=mirebalais.app.patientRegistration.checkin.label",
                            null),
                    sessionLocationHasTag(LocationTags.CHECKIN_LOCATION)));
        }

        extensions.add(visitAction(Extensions.CHECK_IN_VISIT_ACTION,
                "mirebalais.task.checkin.label",
                "icon-check-in",
                "link",
                enterSimpleHtmlFormLink(determineHtmlFormPath(config, "checkin")),
                        "Task: mirebalais.checkinForm",
                        sessionLocationHasTag(LocationTags.CHECKIN_LOCATION)));

        extensions.add(overallRegistrationAction(Extensions.CHECK_IN_REGISTRATION_ACTION,
                "mirebalais.task.checkin.label",
                "icon-check-in",
                "link",
                enterSimpleHtmlFormLink(determineHtmlFormPath(config, "liveCheckin")) + andCreateVisit(),
                    "Task: mirebalais.checkinForm",
                    sessionLocationHasTag(LocationTags.CHECKIN_LOCATION)));

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        registerTemplateForEncounterType(EncounterTypes.CHECK_IN,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-check-in", true, true,
                editSimpleHtmlFormLink(determineHtmlFormPath(config, "checkin")), null);
    }

    private void enableVitals() {

        if (config.isComponentEnabled(Components.UHM_VITALS)) {
            // custom vitals app used in Mirebalais
            apps.add(addToHomePage(findPatientTemplateApp(Apps.UHM_VITALS,
                                    "mirebalais.outpatientVitals.title",
                                    "icon-vitals",
                                    "App: mirebalais.outpatientVitals",
                                    "/mirebalais/outpatientvitals/patient.page?patientId={{patientId}}",
                                    null),
                    sessionLocationHasTag(LocationTags.VITALS_LOCATION)));
        }
        else {
            apps.add(addToHomePage(app(Apps.VITALS,
                    "pihcore.vitalsList.title",
                    "icon-vitals",
                    "/pihcore/vitals/vitalsList.page",
                    "App: mirebalais.outpatientVitals",  // TODO remane this permission to not be mirebalais-specific?
                    null)));

        }

        extensions.add(visitAction(Extensions.VITALS_CAPTURE_VISIT_ACTION,
                "mirebalais.task.vitals.label",
                "icon-vitals",
                "link",
                enterSimpleHtmlFormLink("pihcore:htmlforms/vitals.xml"),
                Privileges.TASK_EMR_ENTER_VITALS_NOTE.privilege(),
                and(sessionLocationHasTag(LocationTags.VITALS_LOCATION), patientHasActiveVisit())));

        apps.add(addToClinicianDashboardSecondColumn(app(Apps.MOST_RECENT_VITALS,
                        "mirebalais.mostRecentVitals.label",
                        "icon-vitals",
                        null,
                        "App: mirebalais.outpatientVitals",
                        objectNode("encounterDateLabel", "mirebalais.mostRecentVitals.encounterDateLabel",
                                "encounterTypeUuid", EncounterTypes.VITALS.uuid(),
                                "editable", Boolean.TRUE,
                                "edit-provider", "htmlformentryui",
                                "edit-fragment", "htmlform/editHtmlFormWithSimpleUi",
                                "definitionUiResource", "pihcore:htmlforms/vitals.xml",
                                "returnProvider", "coreapps",
                                "returnPage", "clinicianfacing/patient")),
                "coreapps",
                "encounter/mostRecentEncounter"));

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        registerTemplateForEncounterType(EncounterTypes.VITALS,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-vitals", null, true,
                editSimpleHtmlFormLink("pihcore:htmlforms/vitals.xml"), null);

    }

    private void enableConsult() {

        extensions.add(visitAction(Extensions.CONSULT_NOTE_VISIT_ACTION,
                "emr.clinic.consult.title",
                "icon-stethoscope",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/outpatientConsult.xml"),
                null,
                and(sessionLocationHasTag(LocationTags.CONSULT_NOTE_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_CONSULT_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        extensions.add(encounterTemplate(EncounterTemplates.CONSULT, "mirebalais", "patientdashboard/encountertemplate/consultEncounterTemplate"));

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        registerTemplateForEncounterType(EncounterTypes.CONSULTATION,
                findExtensionById(EncounterTemplates.CONSULT), "icon-stethoscope", null, true, null, null);
    }

    private void  enableEDConsult() {

        extensions.add(visitAction(Extensions.ED_CONSULT_NOTE_VISIT_ACTION,
                "emr.ed.consult.title",
                "icon-stethoscope",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/edNote.xml"),
                null,
                and(sessionLocationHasTag(LocationTags.ED_NOTE_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_ED_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));
    }

    private void enableADT() {

        apps.add(addToHomePage(app(Apps.AWAITING_ADMISSION,
                "coreapps.app.awaitingAdmission.label",
                "icon-list-ul",
                "coreapps/adt/awaitingAdmission.page?app=" + Apps.AWAITING_ADMISSION,
                "App: coreapps.awaitingAdmission",
                objectNode("patientPageUrl", config.getDashboardUrl()))));

        apps.add(addToHomePage(app(Apps.INPATIENTS,
                                "mirebalaisreports.app.inpatients.label",
                                "icon-list-ol",
                                "mirebalaisreports/inpatientList.page",
                                "App: emr.inpatients",
                                null),
                sessionLocationHasTag(LocationTags.INPATIENTS_APP_LOCATION)));

        extensions.add(awaitingAdmissionAction(Extensions.ADMISSION_FORM_AWAITING_ADMISSION_ACTION,
                "mirebalais.task.admit.label",
                "icon-h-sign",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/admissionNote.xml&returnProvider=coreapps&returnPage=adt/awaitingAdmission&returnLabel=coreapps.app.awaitingAdmission.label"),
                "Task: emr.enterAdmissionNote",
                null));

        extensions.add(awaitingAdmissionAction(Extensions.DENY_ADMISSION_FORM_AWAITING_ADMISSION_ACTION,
                "uicommons.cancel",
                "icon-remove",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/cancelAdmission.xml&returnProvider=coreapps&returnPage=adt/awaitingAdmission"),
                "Task: emr.enterAdmissionNote",
                null));

        extensions.add(visitAction(Extensions.ADMISSION_NOTE_VISIT_ACTION,
                "mirebalais.task.admit.label",
                "icon-h-sign",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/admissionNote.xml"),
                null,
                and(sessionLocationHasTag(LocationTags.ADMISSION_NOTE_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_ADMISSION_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        // TODO will these be needed after we stop using the old patient visits page view?
        registerTemplateForEncounterType(EncounterTypes.ADMISSION,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-signin", null, true, null, null);

        registerTemplateForEncounterType(EncounterTypes.CANCEL_ADMISSION,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-ban-circle", true, true, null, null);

        registerTemplateForEncounterType(EncounterTypes.TRANSFER,
                findExtensionById(EncounterTemplates.NO_DETAILS), "icon-share", null, true, null, null);

        registerTemplateForEncounterType(EncounterTypes.EXIT_FROM_CARE,
                findExtensionById(EncounterTemplates.NO_DETAILS), "icon-signout", null, true, null, null);
    }

    private void enableDeathCertificate() {

        extensions.add(overallAction(Extensions.DEATH_CERTIFICATE_OVERALL_ACTION,
                "mirebalais.deathCertificate.death_certificate",
                "icon-remove-circle",
                "link",
                enterSimpleHtmlFormLink("pihcore:htmlforms/deathCertificate.xml"),
                "Task: mirebalais.enterDeathCertificate",
                "!patient.person.dead"
        ));

        extensions.add(fragmentExtension(Extensions.DEATH_CERTIFICATE_HEADER_EXTENSION,
                "mirebalais",
                "deathcertificate/headerLink",
                "Task: mirebalais.enterDeathCertificate",
                ExtensionPoints.DEATH_INFO_HEADER,
                null));

        addFeatureToggleToExtension(findExtensionById(Extensions.DEATH_CERTIFICATE_OVERALL_ACTION), "deathNote");
        addFeatureToggleToExtension(findExtensionById(Extensions.DEATH_CERTIFICATE_HEADER_EXTENSION), "deathNote");
    }

    private void enableRadiology() {

        extensions.add(dashboardTab(Extensions.RADIOLOGY_TAB,
                "radiologyapp.radiology.label",
                "Task: org.openmrs.module.radiologyapp.tab",
                "radiologyapp",
                "radiologyTab"));

        extensions.add(visitAction(Extensions.ORDER_XRAY_VISIT_ACTION,
                "radiologyapp.task.order.CR.label",
                "icon-x-ray",
                "link",
                "radiologyapp/orderRadiology.page?patientId={{patient.uuid}}&visitId={{visit.id}}&modality=CR",
                null,
                and(sessionLocationHasTag(LocationTags.ORDER_RADIOLOGY_STUDY_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_RADIOLOGYAPP_ORDER_XRAY), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_RADIOLOGYAPP_RETRO_ORDER)))));

        extensions.add(visitAction(Extensions.ORDER_CT_VISIT_ACTION,
                "radiologyapp.task.order.CT.label",
                "icon-x-ray",
                "link",
                "radiologyapp/orderRadiology.page?patientId={{patient.uuid}}&visitId={{visit.id}}&modality=Ct",
                null,
                and(sessionLocationHasTag(LocationTags.ORDER_RADIOLOGY_STUDY_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_RADIOLOGYAPP_ORDER_CT), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_RADIOLOGYAPP_RETRO_ORDER)))));

        extensions.add(visitAction(Extensions.ORDER_ULTRASOUND_VISIT_ACTION,
                "radiologyapp.task.order.US.label",
                "icon-x-ray",
                "link",
                "radiologyapp/orderRadiology.page?patientId={{patient.uuid}}&visitId={{visit.id}}&modality=US",
                null,
                and(sessionLocationHasTag(LocationTags.ORDER_RADIOLOGY_STUDY_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_RADIOLOGYAPP_ORDER_US), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_RADIOLOGYAPP_RETRO_ORDER)))));

        if (config.isComponentEnabled(Components.CLINICIAN_DASHBOARD)) {
            apps.add(addToClinicianDashboardFirstColumn(app(Apps.RADIOLOGY_ORDERS_APP,
                            "radiologyapp.app.orders",
                            "icon-camera",
                            "null",
                            "Task: org.openmrs.module.radiologyapp.tab",
                            null),
                    "radiologyapp", "radiologyOrderSection"));

            apps.add(addToClinicianDashboardFirstColumn(app(Apps.RADIOLOGY_APP,
                    "coreapps.clinicianfacing.radiology",
                    "icon-camera",
                    "null",
                    "Task: org.openmrs.module.radiologyapp.tab",
                    null),
                    "radiologyapp", "radiologySection"));
        }

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        registerTemplateForEncounterType(EncounterTypes.RADIOLOGY_ORDER,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-x-ray");

        registerTemplateForEncounterType(EncounterTypes.RADIOLOGY_STUDY,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-x-ray");

        registerTemplateForEncounterType(EncounterTypes.RADIOLOGY_REPORT,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-x-ray");
    }

    private void enableDispensing() {

        // TODO change this to use the coreapps find patient app?
        apps.add(addToHomePage(app(Apps.DISPENSING,
                        "dispensing.app.label",
                        "icon-medicine",
                        "dispensing/findPatient.page",
                        "App: dispensing.app.dispense",
                        null),
                sessionLocationHasTag(LocationTags.DISPENSING_LOCATION)));

        extensions.add(visitAction(Extensions.DISPENSE_MEDICATION_VISIT_ACTION,
                "dispensing.app.label",
                "icon-medicine",
                "link",
                enterStandardHtmlFormLink("dispensing:htmlforms/dispensing.xml"),
                "Task: mirebalais.dispensing",
                sessionLocationHasTag(LocationTags.DISPENSING_LOCATION)));

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        registerTemplateForEncounterType(EncounterTypes.MEDICATION_DISPENSED,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-medicine", true, true, null, "bad21515-fd04-4ff6-bfcd-78456d12f168");

    }

    private void enableSurgery() {

        extensions.add(visitAction(Extensions.SURGICAL_NOTE_VISIT_ACTION,
                "mirebalais.task.surgicalOperativeNote.label",
                "icon-paste",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/surgicalPostOpNote.xml"),
                Privileges.TASK_EMR_ENTER_SURGICAL_NOTE.privilege(),
                sessionLocationHasTag(LocationTags.SURGERY_NOTE_LOCATION)));

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        registerTemplateForEncounterType(EncounterTypes.POST_OPERATIVE_NOTE,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-paste", true, true, null, "9b135b19-7ebe-4a51-aea2-69a53f9383af");
        }

    private void enableOverviewReports() {

        // both overviewReports and dataExports define this, so make sure if both are turned on we don't config it twice
        if (findAppById(Apps.REPORTS) == null) {
            apps.add(addToHomePage(app(Apps.REPORTS,
                    "reportingui.reportsapp.home.title",
                    "icon-list-alt",
                    "reportingui/reportsapp/home.page",
                    "App: reportingui.reports",
                    null)));
        }

        for (BaseReportManager report : Context.getRegisteredComponents(BaseReportManager.class)) {
            if (report.getCountries().contains(config.getCountry())  || report.getSites().contains(config.getSite())) {

                if (report.getCategory() == BaseReportManager.Category.OVERVIEW) {
                    extensions.add(overviewReport("mirebalaisreports.overview." + report.getName(),
                            report.getMessageCodePrefix() + "name",
                            report.getUuid(),
                            "App: reportingui.reports",
                            report.getOrder(),
                            "mirebalaisreports-" + report.getName() + "-link"));
                }
                else if (report.getCategory() == BaseReportManager.Category.DAILY) {
                    extensions.add(dailyReport("mirebalaisreports.dailyReports." + report.getName(),
                            report.getMessageCodePrefix() + "name",
                            report.getUuid(),
                            "App: reportingui.reports",
                            report.getOrder(),
                            "mirebalaisreports-" + report.getName() + "-link"));
                }

            }
        }

        // TODO: Get rid of these hacked-in reports in favor of proper configuration
        // quick-and-dirty reports for Liberia
        if (config.getCountry() == ConfigDescriptor.Country.LIBERIA || config.getCountry() == ConfigDescriptor.Country.SIERRA_LEONE) {
            extensions.add(extension(Extensions.REGISTRATION_SUMMARY_BY_AGE_REPORT,
                    "mirebalaisreports.registrationoverview.title",
                    null,
                    "link",
                    "mirebalaisreports/registrationsByAge.page",
                    "App: reportingui.reports",
                    null,
                    ExtensionPoints.REPORTING_OVERVIEW_REPORTS,
                    1,
                    map("linkId", "mirebalaisreports-registrationoverview-link")));

            extensions.add(extension(Extensions.CHECK_IN_SUMMARY_BY_AGE_REPORT,
                    "mirebalaisreports.checkinoverview.title",
                    null,
                    "link",
                    "mirebalaisreports/checkInsByAge.page",
                    "App: reportingui.reports",
                    null,
                    ExtensionPoints.REPORTING_OVERVIEW_REPORTS,
                    1,
                    map("linkId", "mirebalaisreports-checkinoverview-link")));

        } else if ( config.getCountry() == ConfigDescriptor.Country.HAITI ) {
            // special non-coded report in it's own section for Haiti
            extensions.add(extension(Extensions.NON_CODED_DIAGNOSES_DATA_QUALITY_REPORT,
                    "mirebalaisreports.noncodeddiagnoses.name",
                    null,
                    "link",
                    "mirebalaisreports/nonCodedDiagnoses.page",
                    "App: reportingui.reports",
                    null,
                    ExtensionPoints.REPORTING_DATA_QUALITY,
                    0,
                    map("linkId", "mirebalaisreports-nonCodedDiagnosesReport-link")));

            if (config.getSite() == ConfigDescriptor.Site.MIREBALAIS) {
                // TODO in particular, get rid of this hacked in report, seems like it should be easy enough to do?
                // custom daily inpatients report
                extensions.add(extension(Extensions.DAILY_INPATIENTS_OVERVIEW_REPORT,
                        "mirebalaisreports.inpatientStatsDailyReport.name",
                        null,
                        "link",
                        "mirebalaisreports/inpatientStatsDailyReport.page",
                        "App: reportingui.reports",
                        null,
                        ExtensionPoints.REPORTING_OVERVIEW_REPORTS,
                        3,
                        map("linkId", "mirebalaisreports-inpatientDailyReport-link")));
            }

        }
    }

    private void enableMonitoringReports() {

        // overReports, monitoring reports, and dataExports define this, so make sure if both are turned on we don't config it twice
        if (findAppById(Apps.REPORTS) == null) {
            apps.add(addToHomePage(app(Apps.REPORTS,
                    "reportingui.reportsapp.home.title",
                    "icon-list-alt",
                    "reportingui/reportsapp/home.page",
                    "App: reportingui.reports",
                    null)));
        }

        for (BaseReportManager report : Context.getRegisteredComponents(BaseReportManager.class)) {
            if (report.getCategory() == BaseReportManager.Category.MONITORING &&
                    (report.getCountries().contains(config.getCountry())  || report.getSites().contains(config.getSite()))) {
                extensions.add(monitoringReport("mirebalaisreports.monitoring." + report.getName(),
                        report.getMessageCodePrefix() + "name",
                        report.getUuid(),
                        "App: reportingui.reports",
                        report.getOrder(),
                        "mirebalaisreports-" + report.getName() + "-link"));
            }
        }

    }

    private void enableDataExports() {

        // overReports, monitoring reports, and dataExports define this, so make sure if both are turned on we don't config it twice
        if (findAppById(Apps.REPORTS) == null) {
            apps.add(addToHomePage(app(Apps.REPORTS,
                    "reportingui.reportsapp.home.title",
                    "icon-list-alt",
                    "reportingui/reportsapp/home.page",
                    "App: reportingui.reports",
                    null)));
        }

        extensions.addAll(fullDataExportBuilder.getExtensions());

        for (BaseReportManager report : Context.getRegisteredComponents(BaseReportManager.class)) {
            if (report.getCategory() == BaseReportManager.Category.DATA_EXPORT &&
                (report.getCountries().contains(config.getCountry())  || report.getSites().contains(config.getSite()))) {
                extensions.add(dataExport("mirebalaisreports.dataExports." + report.getName(),
                        report.getMessageCodePrefix() + "name",
                        report.getUuid(),
                        "App: mirebalaisreports.dataexports",
                        report.getOrder(),
                        "mirebalaisreports-" + report.getName() + "-link"));
            }
        }

        // TODO: Replace this with property configuration in config
        if (config.getSite().equals(ConfigDescriptor.Site.MIREBALAIS)) {

            // custom data export report LQAS report report
            extensions.add(extension(Extensions.LQAS_DATA_EXPORT,
                    "mirebalaisreports.lqasdiagnoses.name",
                    null,
                    "link",
                    "mirebalaisreports/lqasDiagnoses.page",
                    "App: mirebalaisreports.dataexports",
                    null,
                    ExtensionPoints.REPORTING_DATA_EXPORT,
                    REPORTING_DATA_EXPORT_REPORTS_ORDER.indexOf(MirebalaisReportsProperties.LQAS_DIAGNOSES_REPORT_DEFINITION_UUID) + 1000,
                    map("linkId", "mirebalaisreports-lqasDiagnosesReport-link")));
        }

        extensions.add(extension(Extensions.REPORTING_AD_HOC_ANALYSIS,
                "reportingui.adHocAnalysis.label",
                null,
                "link",
                "reportingui/adHocManage.page",
                "App: reportingui.adHocAnalysis",
                null,
                ExtensionPoints.REPORTING_DATA_EXPORT,
                9999,
                null));

        addFeatureToggleToExtension(findExtensionById(Extensions.REPORTING_AD_HOC_ANALYSIS), "reporting_adHocAnalysis");

    }

    private void enableArchives() {

        apps.add(addToHomePage(app(Apps.ARCHIVES_ROOM,
                "paperrecord.app.archivesRoom.label",
                "icon-folder-open",
                "paperrecord/archivesRoom.page",
                "App: emr.archivesRoom",
                null)));

        extensions.add(overallAction(Extensions.REQUEST_PAPER_RECORD_OVERALL_ACTION,
                "paperrecord.task.requestPaperRecord.label",
                "icon-folder-open",
                "script",
                "showRequestChartDialog()",
                "Task: emr.requestPaperRecord",
                null));

        extensions.add(overallAction(Extensions.PRINT_ID_CARD_OVERALL_ACTION,
                "paperrecord.task.printIdCardLabel.label",
                "icon-print",
                "script",
                "printIdCardLabel()",
                "Task: emr.printLabels",
                null));

        extensions.add(overallAction(Extensions.PRINT_PAPER_FORM_LABEL_OVERALL_ACTION,
                "paperrecord.task.printPaperFormLabel.label",
                "icon-print",
                "script",
                "printPaperFormLabel()",
                "Task: emr.printLabels",
                null));

        addPaperRecordActionsIncludesIfNeeded();
    }

    public void enableWristbands() {

        extensions.add(overallAction(Extensions.PRINT_WRISTBAND_OVERALL_ACTION,
                "mirebalais.printWristband",
                "icon-print",
                "script",
                "printWristband()",
                "Task: emr.printWristband",
                null));

        // this provides the javascript the backs the print wrist action button
        extensions.add(fragmentExtension(Extensions.PRINT_WRISTBAND_ACTION_INCLUDES,
                "mirebalais",
                "wristband/printWristband",
                null,
                ExtensionPoints.DASHBOARD_INCLUDE_FRAGMENTS,
                null));

    }

    private void enableAppointmentScheduling() {

        AppDescriptor apppointmentScheduling = app(Apps.APPOINTMENT_SCHEDULING_HOME,
                "appointmentschedulingui.scheduleAppointment.new.title",
                "icon-calendar",
                "appointmentschedulingui/home.page",
                "App: appointmentschedulingui.home",
                null);

        apps.add(addToHomePage(apppointmentScheduling));

        apps.add(findPatientTemplateApp(Apps.SCHEDULE_APPOINTMENT,
                "appointmentschedulingui.scheduleAppointment.buttonTitle",
                "icon-calendar",
                "Task: appointmentschedulingui.bookAppointments",
                "/appointmentschedulingui/manageAppointments.page?patientId={{patientId}}&breadcrumbOverride={{breadcrumbOverride}}",
                arrayNode(objectNode("icon", "icon-home", "link", "/index.htm"),
                        objectNode("label", "appointmentschedulingui.home.title", "link", "/appointmentschedulingui/home.page"),
                        objectNode("label", "appointmentschedulingui.scheduleAppointment.buttonTitle"))));

        extensions.add(overallAction(Extensions.SCHEDULE_APPOINTMENT_OVERALL_ACTION,
                "appointmentschedulingui.scheduleAppointment.new.title",
                "icon-calendar",
                "link",
                "appointmentschedulingui/manageAppointments.page?patientId={{patient.uuid}}",
                "Task: appointmentschedulingui.bookAppointments",
                null));

        extensions.add(overallAction(Extensions.REQUEST_APPOINTMENT_OVERALL_ACTION,
                "appointmentschedulingui.requestAppointment.label",
                "icon-calendar",
                "link",
                "appointmentschedulingui/requestAppointment.page?patientId={{patient.uuid}}",
                "Task: appointmentschedulingui.requestAppointments",
                null));

        extensions.add(dashboardTab(Extensions.APPOINTMENTS_TAB,
                "appointmentschedulingui.appointmentsTab.label",
                "App: appointmentschedulingui.viewAppointments",
                "appointmentschedulingui",
                "appointmentsTab"));

        if (config.isComponentEnabled(Components.CLINICIAN_DASHBOARD)) {
            addToClinicianDashboardFirstColumn(apppointmentScheduling,
                    "appointmentschedulingui", "miniPatientAppointments");
        }

    }

    private void enableSystemAdministration() {

        if (findAppById(Apps.SYSTEM_ADMINISTRATION) == null) {
            apps.add(addToHomePage(app(Apps.SYSTEM_ADMINISTRATION,
                    "coreapps.app.system.administration.label",
                    "icon-cogs",
                    "coreapps/systemadministration/systemAdministration.page",
                    "App: emr.systemAdministration",
                    null)));
        }

        apps.add(addToSystemAdministrationPage(app(Apps.MANAGE_ACCOUNTS,
                "emr.task.accountManagement.label",
                "icon-book",
                "emr/account/manageAccounts.page",
                "App: emr.systemAdministration",
                null)));

        apps.add(addToSystemAdministrationPage(app(Apps.MERGE_PATIENTS,
                "coreapps.mergePatientsLong",
                "icon-group",
                "coreapps/datamanagement/mergePatients.page?app=coreapps.mergePatients",
                "App: emr.systemAdministration",
                objectNode("breadcrumbs", arrayNode(objectNode("icon", "icon-home", "link", "/index.htm"),
                        objectNode("label", "coreapps.app.systemAdministration.label", "link", "/coreapps/systemadministration/systemAdministration.page"),
                        objectNode("label", "coreapps.mergePatientsLong")),
                        "dashboardUrl", (config.getAfterMergeUrl() !=null ) ? (config.getAfterMergeUrl()):(config.getDashboardUrl()) ))));

        apps.add(addToSystemAdministrationPage(app(Apps.REGISTER_TEST_PATIENT,
                "emr.testPatient.registration",
                "icon-register",
                "mirebalais/patientRegistration/appRouter.page?task=patientRegistration&testPatient=true",
                "App: emr.systemAdministration",
                null)));

        apps.add(addToSystemAdministrationPage(app(Apps.FEATURE_TOGGLES,
                "emr.advancedFeatures",
                "icon-search",
                "mirebalais/toggles.page",
                "App: emr.systemAdministration",
                null)));

        addFeatureToggleToApp(findAppById(Apps.REGISTER_TEST_PATIENT), "registerTestPatient");

    }

    private void enableManagePrinters() {

        if (findAppById(Apps.SYSTEM_ADMINISTRATION) == null) {
            apps.add(addToHomePage(app(Apps.SYSTEM_ADMINISTRATION,
                    "coreapps.app.system.administration.label",
                    "icon-cogs",
                    "coreapps/systemadministration/systemAdministration.page",
                    "App: emr.systemAdministration",
                    null)));
        }

        apps.add(addToSystemAdministrationPage(app(Apps.PRINTER_ADMINISTRATION,
                "printer.administration",
                "icon-print",
                "printer/printerAdministration.page",
                "App: emr.systemAdministration",
                null)));

    }

    private void enableMyAccount() {

        apps.add(addToHomePage(app(Apps.MY_ACCOUNT,
                "emr.app.system.administration.myAccount.label",
                "icon-cog",
                "emr/account/myAccount.page",
                null, null)));

    }

    private void enablePatientRegistration() {

        apps.add(addToHomePage(patientRegistrationApp.getAppDescriptor(config),
                sessionLocationHasTag(LocationTags.REGISTRATION_LOCATION)));

        if (featureToggles.isFeatureEnabled("additionalHaitiIdentifiers")) {
            // we currently only have additioanl identifiers in Haiti, excluding Mental Health
            if (config.getCountry().equals(ConfigDescriptor.Country.HAITI) &&
                    !ConfigDescriptor.Specialty.MENTAL_HEALTH.equals(config.getSpecialty())) {  // reversed to make this null safe
                apps.add(addToRegistrationSummarySecondColumnContent(app(Apps.ADDITIONAL_IDENTIFIERS,
                        "zl.registration.patient.additionalIdentifiers",
                        "icon-user",
                        null,
                        "App: registrationapp.registerPatient",
                        null),
                        "registrationapp",
                        "summary/section",
                        map("sectionId", "patient-identification-section")));
            }
        }

        apps.add(addToRegistrationSummaryContent(app(Apps.MOST_RECENT_REGISTRATION_SUMMARY,
                        "mirebalais.mostRecentRegistration.label",
                        "icon-user",
                        null,
                        "App: registrationapp.registerPatient",
                        objectNode("encounterDateLabel", "mirebalais.mostRecentRegistration.encounterDateLabel",
                                "encounterTypeUuid", EncounterTypes.PATIENT_REGISTRATION.uuid(),
                                "definitionUiResource", determineHtmlFormPath(config, "patientRegistration-rs"),
                                "editable", true,
                                "creatable", true)),
                "coreapps",
                "encounter/mostRecentEncounter"));

        if (config.isComponentEnabled(Components.RELATIONSHIPS)) {
            apps.add(addToRegistrationSummarySecondColumnContent(app(Apps.RELATIONSHIPS_REGISTRATION_SUMMARY,
                    "pihcore.relationshipsDashboardWidget.label",
                    "icon-group",
                    null,
                    null, // TODO restrict by privilege or location)
                    objectNode(
                            "widget", "relationships",
                            "baseAppPath", "/registrationapp",
                            "editable", "true",
                            "editPrivilege", CoreAppsConstants.PRIVILEGE_EDIT_RELATIONSHIPS,
                            "dashboardPage", "/registrationapp/registrationSummary.page?patientId={{patientUuid}}&appId=registrationapp.registerPatient",
                            "providerPage", "/coreapps/providermanagement/editProvider.page?personUuid={{personUuid}}",
                            "includeRelationshipTypes", RelationshipTypeBundle.RelationshipTypes.SPOUSE_PARTNER
                                    + "," + PihCoreConstants.RELATIONSHIP_SIBLING
                                    + "," + PihCoreConstants.RELATIONSHIP_PARENT_CHILD,
                            "icon", "icon-group",
                            "label", "pihcore.relationshipsDashboardWidget.label"
                    )),
                    "coreapps", "dashboardwidgets/dashboardWidget"));

            // TODO remove feature toggle after we have launched relationsips
            addFeatureToggleToApp(findAppById(Apps.RELATIONSHIPS_REGISTRATION_SUMMARY), "relationships");
        }


        if ( config.getCountry().equals(ConfigDescriptor.Country.HAITI) &&
                !ConfigDescriptor.Specialty.MENTAL_HEALTH.equals(config.getSpecialty())) {  // reversed to make this null safe
            apps.add(addToRegistrationSummaryContent(app(Apps.MOST_RECENT_REGISTRATION_INSURANCE,
                    "zl.registration.patient.insurance.insuranceName.label",
                    "icon-user",
                    null,
                    "App: registrationapp.registerPatient",
                    objectNode("encounterDateLabel", "mirebalais.mostRecentRegistration.encounterDateLabel",
                            "encounterTypeUuid", EncounterTypes.PATIENT_REGISTRATION.uuid(),
                            "definitionUiResource", determineHtmlFormPath(config, "patientRegistration-insurance"),
                            "editable", true)),
                    "coreapps",
                    "encounter/mostRecentEncounter"));
        }

        apps.add(addToRegistrationSummaryContent(app(Apps.MOST_RECENT_REGISTRATION_SOCIAL,
                        "zl.registration.patient.social.label",
                        "icon-user",
                        null,
                        "App: registrationapp.registerPatient",
                        objectNode("encounterDateLabel", "mirebalais.mostRecentRegistration.encounterDateLabel",
                                "encounterTypeUuid", EncounterTypes.PATIENT_REGISTRATION.uuid(),
                                "definitionUiResource", determineHtmlFormPath(config, "patientRegistration-social"),
                                "editable", true)),
                "coreapps",
                "encounter/mostRecentEncounter"));


        apps.add(addToRegistrationSummarySecondColumnContent(app(Apps.MOST_RECENT_REGISTRATION_CONTACT,
                        "zl.registration.patient.contactPerson.label",
                        "icon-user",
                        null,
                        "App: registrationapp.registerPatient",
                        objectNode("encounterDateLabel", "mirebalais.mostRecentRegistration.encounterDateLabel",
                                "encounterTypeUuid", EncounterTypes.PATIENT_REGISTRATION.uuid(),
                                "definitionUiResource", determineHtmlFormPath(config, "patientRegistration-contact"),
                                "editable", true)),
                "coreapps",
                "encounter/mostRecentEncounter"));


        if (config.isComponentEnabled(Components.CHECK_IN)) {
            apps.add(addToRegistrationSummarySecondColumnContent(app(Apps.MOST_RECENT_CHECK_IN,
                    "pihcore.mostRecentCheckin.label",
                    "icon-ok",
                    null,
                    "App: registrationapp.registerPatient",
                    objectNode("encounterDateLabel", "pihcore.mostRecentCheckin.encounterDateLabel",
                            "encounterTypeUuid", EncounterTypes.CHECK_IN.uuid(),
                            "definitionUiResource", determineHtmlFormPath(config, "checkin"),
                            "editable", true,
                            "edit-provider", "htmlformentryui",
                            "edit-fragment", "htmlform/editHtmlFormWithSimpleUi")),
                    "coreapps",
                    "encounter/mostRecentEncounter"));
        }

        if (config.isComponentEnabled(Components.ID_CARD_PRINTING)) {
            apps.add(addToRegistrationSummarySecondColumnContent(app(Apps.ID_CARD_PRINTING_STATUS,
                            "zl.registration.patient.idcard.status",
                            "icon-barcode",
                            null,
                            "App: registrationapp.registerPatient",
                            null),
                    "mirebalais",
                    "patientRegistration/idCardStatus"));
        }

        extensions.add(overallRegistrationAction(Extensions.REGISTER_NEW_PATIENT,
                "registrationapp.home",
                "icon-user",
                "link",
                "registrationapp/findPatient.page?appId=" + Apps.PATIENT_REGISTRATION,
                "App: registrationapp.registerPatient",
                sessionLocationHasTag(LocationTags.REGISTRATION_LOCATION)));

        extensions.add(overallRegistrationAction(Extensions.MERGE_INTO_ANOTHER_PATIENT,
                "coreapps.mergePatientsShort",
                "icon-group",
                "link",
                "coreapps/datamanagement/mergePatients.page?app=coreapps.mergePatients&patient1={{patient.patientId}}",
                "App: registrationapp.registerPatient",
                null));

        if (config.isComponentEnabled(Components.CLINICIAN_DASHBOARD)) {
            extensions.add(overallRegistrationAction(Extensions.CLINICIAN_FACING_PATIENT_DASHBOARD,
                    "registrationapp.clinicalDashboard",
                    "icon-stethoscope",
                    "link",
                    "coreapps/clinicianfacing/patient.page?patientId={{patient.patientId}}&appId=" + Apps.PATIENT_REGISTRATION,
                    "App: coreapps.patientDashboard",
                    null));

            extensions.add(overallAction(Extensions.REGISTRATION_SUMMARY_OVERALL_ACTION,
                    "registrationapp.patient.registrationSummary",
                    "icon-user",
                    "link",
                    "registrationapp/registrationSummary.page?patientId={{patient.patientId}}&appId=" + Apps.PATIENT_REGISTRATION,
                    "App: registrationapp.registerPatient",
                    null));
        }

        if (config.isComponentEnabled(Components.VISIT_MANAGEMENT)) {
            extensions.add(overallRegistrationAction(Extensions.VISITS_DASHBOARD,
                    "pihcore.visitDashboard",
                    "icon-user",
                    "link",
                    patientVisitsPageUrl,
                    "App: coreapps.patientDashboard",
                    null));
        }

        if (config.isComponentEnabled(Components.ARCHIVES)) {
            extensions.add(overallRegistrationAction(Extensions.PRINT_PAPER_FORM_LABEL,
                    "paperrecord.task.printPaperFormLabel.label",
                    "icon-print",
                    "script",
                    "printPaperFormLabel()",
                    "Task: emr.printLabels",
                    null));
        }

        if (config.isComponentEnabled(Components.ID_CARD_PRINTING)) {
            extensions.add(overallRegistrationAction(Extensions.PRINT_ID_CARD_REGISTRATION_ACTION,
                "zl.registration.patient.idcard.label",
                "icon-barcode",
                "link",
                "mirebalais/patientRegistration/printIdCard.page?patientId={{patient.patientId}}",
                "App: registrationapp.registerPatient",
                null));
        }

        // TODO hack for Sierra Leone, clean up if we actually use this, make an actual component for for, remove if we don't use it
        if (config.getCountry().equals(ConfigDescriptor.Country.SIERRA_LEONE)) {
            extensions.add(overallRegistrationAction("printLabel",
                    "Print Label",  // TODO convert to message code
                    "icon-print",
                    "script",
                    "printLabel()",
                    "Task: emr.printLabels",
                    null));
            extensions.add(fragmentExtension("printLabelIncludes",
                    "mirebalais",
                    "patientRegistration/printLabel",
                    null,
                    ExtensionPoints.DASHBOARD_INCLUDE_FRAGMENTS,
                    null));
        }


        addPaperRecordActionsIncludesIfNeeded();

    }

    // legacy MPI used in Mirebalais to connect to Lacolline
    private void enableLegacyMPI() {
        apps.add(addToHomePageWithoutUsingRouter(app(Apps.LEGACY_MPI,
                "mirebalais.mpi.title",
                "icon-zoom-in",
                "mirebalais/mpi/findPatient.page",
                "App: mirebalais.mpi",
                null)) );
    }

    private void enableClinicianDashboard() {

        apps.add(app(Apps.CLINICIAN_DASHBOARD,
                "mirebalais.app.clinicianDashboard.label",
                "icon-medkit",
                "coreapps/clinicianfacing/patient.page?app=" + Apps.CLINICIAN_DASHBOARD,
                CoreAppsConstants.PRIVILEGE_PATIENT_DASHBOARD,
                objectNode(
                        "visitUrl", patientVisitsPageWithSpecificVisitUrl,
                        "visitsUrl", patientVisitsPageUrl
                )));

        apps.add(addToClinicianDashboardFirstColumn(app(Apps.VISITS_SUMMARY,
                        "coreapps.clinicianfacing.visits",
                        "icon-calendar",
                        null,
                        null,
                        null),
                "coreapps", "clinicianfacing/visitsSection"));

        // link for new pihcore visit view
        //"visitUrl", "pihcore/visit/visit.page?visit={{visit.uuid}}"

     /*   if (config.isComponentEnabled(CustomAppLoaderConstants.Components.PRESCRIPTIONS)) {
            // TODO figure out how to add icon-pill to this
            // TODO we should actually define an app here, not use the existing app
            addToClinicianDashboardSecondColumn(app, "orderentryui", "patientdashboard/activeDrugOrders");
        }
*/
    }

    private void enableAllergies() {
        apps.add(addToClinicianDashboardSecondColumn(app(Apps.ALLERGY_SUMMARY,
                        "allergyui.allergies",
                        "icon-medical",
                        null,
                        null,
                        null),
                "allergyui", "allergies"));
    }

    private void enableOncology() {

        extensions.add(visitAction(Extensions.ONCOLOGY_CONSULT_NOTE_VISIT_ACTION,
                "pih.task.oncologyConsultNote.label",
                "icon-paste",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/oncologyConsult.xml"),
                Privileges.TASK_EMR_ENTER_ONCOLOGY_CONSULT_NOTE.privilege(),
                and(sessionLocationHasTag(LocationTags.ONCOLOGY_CONSULT_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_ONCOLOGY_CONSULT_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        // will we need this template after we stop using old patient visits view?
        registerTemplateForEncounterType(EncounterTypes.ONCOLOGY_CONSULT,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-paste", true, true,
                null, EncounterRoleBundle.EncounterRoles.CONSULTING_CLINICIAN);

        extensions.add(visitAction(Extensions.ONCOLOGY_INITIAL_VISIT_ACTION,
                "pih.task.oncologyInitialConsult.label",
                "icon-paste",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/oncologyIntake.xml"),
                Privileges.TASK_EMR_ENTER_ONCOLOGY_CONSULT_NOTE.privilege(),
                and(sessionLocationHasTag(LocationTags.ONCOLOGY_CONSULT_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_ONCOLOGY_CONSULT_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        // will we need this template after we stop using old patient visits view?
        registerTemplateForEncounterType(EncounterTypes.ONCOLOGY_INITIAL_VISIT,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-paste", true, true,
                null, EncounterRoleBundle.EncounterRoles.CONSULTING_CLINICIAN);

        extensions.add(visitAction(Extensions.CHEMOTHERAPY_VISIT_ACTION,
                "pih.task.chemotherapySession.label",
                "icon-retweet",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/chemotherapyTreatment.xml"),
                Privileges.TASK_EMR_ENTER_ONCOLOGY_CONSULT_NOTE.privilege(),
                and(sessionLocationHasTag(LocationTags.CHEMOTHERAPY_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_ONCOLOGY_CONSULT_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        registerTemplateForEncounterType(EncounterTypes.CHEMOTHERAPY_SESSION,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-retweet", true, true,
                null, EncounterRoleBundle.EncounterRoles.CONSULTING_CLINICIAN);
    }

    private void enableLabResults() {

        extensions.add(visitAction(Extensions.LAB_RESULTS_VISIT_ACTION,
                "pih.task.labResults.label",
                "icon-beaker",
                "link",
                enterSimpleHtmlFormLink("pihcore:htmlforms/labResults.xml"),
                Privileges.TASK_EMR_ENTER_LAB_RESULTS.privilege(),
                and(sessionLocationHasTag(LocationTags.LAB_RESULTS_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_LAB_RESULTS), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        // will we need this template after we stop using old patient visits view?
        registerTemplateForEncounterType(EncounterTypes.LAB_RESULTS,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-beaker", true, true,
                editSimpleHtmlFormLink(determineHtmlFormPath(config, "labResults")), EncounterRoleBundle.EncounterRoles.CONSULTING_CLINICIAN);

    }

    private void enableNCDs() {

        extensions.add(visitAction(Extensions.NCD_ADULT_INITIAL_VISIT_ACTION,
                "ui.i18n.EncounterType.name." + EncounterTypes.NCD_ADULT_INITIAL_CONSULT.uuid(),
                "icon-heart",
                "link",
                enterStandardHtmlFormLink(determineHtmlFormPath(config, "ncd-adult-initial") + "&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),  // always redirect to visit page after clicking this link
                Privileges.TASK_EMR_ENTER_NCD_CONSULT_NOTE.privilege(),
                and(sessionLocationHasTag(LocationTags.NCD_CONSULT_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_NCD_CONSULT_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        extensions.add(visitAction(Extensions.NCD_ADULT_FOLLOWUP_VISIT_ACTION,
                "ui.i18n.EncounterType.name." + EncounterTypes.NCD_ADULT_FOLLOWUP_CONSULT.uuid(),
                "icon-heart",
                "link",
                enterStandardHtmlFormLink(determineHtmlFormPath(config, "ncd-adult-followup") + "&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),  // always redirect to visit page after clicking this link
                Privileges.TASK_EMR_ENTER_NCD_CONSULT_NOTE.privilege(),
                and(sessionLocationHasTag(LocationTags.NCD_CONSULT_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_NCD_CONSULT_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

    }

    private void enableMentalHealth() {

        extensions.add(visitAction(Extensions.MENTAL_HEALTH_VISIT_ACTION,
                "pih.task.mentalHealth.label",
                "icon-user",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/mentalHealth.xml"),
                Privileges.TASK_EMR_ENTER_MENTAL_HEALTH_NOTE.privilege(),
                and(sessionLocationHasTag(LocationTags.CONSULT_NOTE_LOCATION),
                        or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_MENTAL_HEALTH_NOTE), patientHasActiveVisit()),
                                userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                                and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

                // will we need this template after we stop using old patient visits view?
        registerTemplateForEncounterType(EncounterTypes.MENTAL_HEALTH_ASSESSMENT,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-user", true, true,
                null, EncounterRoleBundle.EncounterRoles.CONSULTING_CLINICIAN);
    }

    private void enableChartSearch() {
        extensions.add(overallAction(Extensions.CHART_SEARCH_OVERALL_ACTION,
                "pihcore.chartSearch.label",
                "icon-search",
                "link",
                "chartsearch/chartsearch.page?patientId={{patient.patientId}}",
                Privileges.TASK_EMR_ENTER_CONSULT_NOTE.privilege(), // TODO correct permission!
                null));
    }

    private void enableWaitingForConsult() {

        apps.add(addToHomePage(app(Apps.WAITING_FOR_CONSULT,
                "pihcore.waitingForConsult.title",
                "icon-stethoscope",
                "pihcore/visit/waitingForConsult.page",
                Privileges.APP_WAITING_FOR_CONSULT.privilege(),
                null)));

    }

    private void enableTodaysVisits() {

        apps.add(addToHomePage(app(Apps.TODAYS_VISITS,
                "pihcore.todaysVisits.title",
                "icon-check-in",
                "pihcore/visit/todaysVisits.page",
                Privileges.APP_TODAYS_VISITS.privilege(),
                null)));

    }


    private void enableCHWApp() {
        if (findAppById(Apps.CHW_MGMT) == null) {
            apps.add(addToHomePage(app(Apps.CHW_MGMT,
                    "chwapp.label",
                    "icon-group",
                    "/coreapps/providermanagement/providerList.page",
                    Privileges.APP_CHW.privilege(),
                    null)));
        }

        addFeatureToggleToApp(findAppById(Apps.CHW_MGMT), "chwApp");
    }


    private void enableEDTriage() {
        apps.add(addToHomePage(findPatientTemplateApp(Apps.ED_TRIAGE,
                "edtriageapp.label",
                "icon-ambulance",
                Privileges.APP_ED_TRIAGE.privilege(),
                "/edtriageapp/edtriageEditPatient.page?patientId={{patientId}}&appId=" + Apps.ED_TRIAGE
                        + "&dashboardUrl=" + config.getDashboardUrl(),
                null),
                sessionLocationHasTag(LocationTags.ED_TRIAGE_LOCATION)));

        extensions.add(visitAction(Extensions.ED_TRIAGE_VISIT_ACTION,
                "ui.i18n.EncounterType.name." + EncounterTypes.EMERGENCY_TRIAGE.uuid(),
                "icon-ambulance",
                "link",
                "/edtriageapp/edtriageEditPatient.page?patientId={{patient.uuid}}&appId=" + Apps.ED_TRIAGE,
                null,
                and(sessionLocationHasTag(LocationTags.ED_TRIAGE_LOCATION), patientHasActiveVisit())));

        // TODO will this be needed after we stop using the old patient visits page view, or is is replaced by encounterTypeConfig?
        registerTemplateForEncounterType(EncounterTypes.EMERGENCY_TRIAGE,
                findExtensionById(EncounterTemplates.ED_TRIAGE), "icon-ambulance", false, true,
                "edtriageapp/edtriageEditPatient.page?patientId={{patient.uuid}}&encounterId={{encounter.uuid}}&appId=edtriageapp.app.triageQueue&returnUrl={{returnUrl}}&breadcrumbOverride={{breadcrumbOverride}}&editable=true",
                null);

        addFeatureToggleToApp(findAppById(Apps.ED_TRIAGE), "edTriage");
        addFeatureToggleToExtension(findExtensionById(Extensions.ED_TRIAGE_VISIT_ACTION), "edTriage");
    }

    private void enableEDTriageQueue() {
        apps.add(addToHomePage(app(Apps.ED_TRIAGE_QUEUE,
                "edtriageapp.queue.label",
                "icon-list-ol",
                "/edtriageapp/edtriageViewQueue.page?appId=" + Apps.ED_TRIAGE_QUEUE
                        + "&dashboardUrl=" + config.getDashboardUrl(),
                Privileges.APP_ED_TRIAGE_QUEUE.privilege(),
                null),
                sessionLocationHasTag(LocationTags.ED_TRIAGE_LOCATION)));

        addFeatureToggleToApp(findAppById(Apps.ED_TRIAGE_QUEUE), "edTriage");
    }

    private void enablePrimaryCare() {

        extensions.add(visitAction(Extensions.PRIMARY_CARE_PEDS_INITIAL_VISIT_ACTION,
                "ui.i18n.EncounterType.name." + EncounterTypes.PRIMARY_CARE_PEDS_INITIAL_CONSULT.uuid(),
                "icon-stethoscope",
                "link",
                enterStandardHtmlFormLink(determineHtmlFormPath(config, "primary-care-peds-initial") + "&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),  // always redirect to visit page after clicking this link
                null,
                and(sessionLocationHasTag(LocationTags.CONSULT_NOTE_LOCATION),
                    or(patientIsChild(), patientAgeUnknown(), patientDoesNotActiveVisit()),
                    or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_CONSULT_NOTE), patientHasActiveVisit()),
                            userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                            and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        extensions.add(visitAction(Extensions.PRIMARY_CARE_PEDS_FOLLOWUP_VISIT_ACTION,
                "ui.i18n.EncounterType.name." + EncounterTypes.PRIMARY_CARE_PEDS_FOLLOWUP_CONSULT.uuid(),
                "icon-stethoscope",
                "link",
                enterStandardHtmlFormLink(determineHtmlFormPath(config, "primary-care-peds-followup") + "&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),  // always redirect to visit page after clicking this link
                null,
                and(sessionLocationHasTag(LocationTags.CONSULT_NOTE_LOCATION),
                    or(patientIsChild(), patientAgeUnknown(), patientDoesNotActiveVisit()),
                    or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_CONSULT_NOTE), patientHasActiveVisit()),
                            userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                            and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        extensions.add(visitAction(Extensions.PRIMARY_CARE_ADULT_INITIAL_VISIT_ACTION,
                "ui.i18n.EncounterType.name." + EncounterTypes.PRIMARY_CARE_ADULT_INITIAL_CONSULT.uuid(),
                "icon-stethoscope",
                "link",
                enterStandardHtmlFormLink(determineHtmlFormPath(config, "primary-care-adult-initial") + "&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),  // always redirect to visit page after clicking this link
                null,
                and(sessionLocationHasTag(LocationTags.CONSULT_NOTE_LOCATION),
                    or(patientIsAdult(), patientAgeUnknown(), patientDoesNotActiveVisit()),
                    or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_CONSULT_NOTE), patientHasActiveVisit()),
                            userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                            and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        extensions.add(visitAction(Extensions.PRIMARY_CARE_ADULT_FOLLOWUP_VISIT_ACTION,
                "ui.i18n.EncounterType.name." + EncounterTypes.PRIMARY_CARE_ADULT_FOLLOWUP_CONSULT.uuid(),
                "icon-stethoscope",
                "link",
                enterStandardHtmlFormLink(determineHtmlFormPath(config, "primary-care-adult-followup") + "&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),  // always redirect to visit page after clicking this link
                null,
                and(sessionLocationHasTag(LocationTags.CONSULT_NOTE_LOCATION),
                    or(patientIsAdult(), patientAgeUnknown(), patientDoesNotActiveVisit()),
                    or(and(userHasPrivilege(Privileges.TASK_EMR_ENTER_CONSULT_NOTE), patientHasActiveVisit()),
                            userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE),
                            and(userHasPrivilege(Privileges.TASK_EMR_RETRO_CLINICAL_NOTE_THIS_PROVIDER_ONLY), patientVisitWithinPastThirtyDays(config))))));

        addFeatureToggleToExtension(findExtensionById(Extensions.PRIMARY_CARE_PEDS_INITIAL_VISIT_ACTION), "primaryCareNote");
        addFeatureToggleToExtension(findExtensionById(Extensions.PRIMARY_CARE_PEDS_FOLLOWUP_VISIT_ACTION), "primaryCareNote");
        addFeatureToggleToExtension(findExtensionById(Extensions.PRIMARY_CARE_ADULT_INITIAL_VISIT_ACTION), "primaryCareNote");
        addFeatureToggleToExtension(findExtensionById(Extensions.PRIMARY_CARE_ADULT_FOLLOWUP_VISIT_ACTION), "primaryCareNote");


        // (allergies no longer part of visit note follow)
        // hacky extension to inject a "next" button into the allergies list page to support our functionality to step through the primary care note sections
        /*extensions.add(fragmentExtension(Extensions.ALLERGY_UI_VISIT_NOTE_NEXT_SUPPORT,
                "pihcore",
                "allergyui/addNextButton",
                null,
                ExtensionPoints.ALLERGIES_PAGE_INCLUDE_PAGE,
                null));*/

    }

    private void enableHIV() {

        // dashboard title configured via b1cb1fc1-5190-4f7a-af08-48870975dafc.custom.title=ZIKA PROGRAM message property

        // ZL HIV forms
        extensions.add(visitAction(Extensions.HIV_ZL_ADULT_INITIAL_VISIT_ACTION,
                "pih.task.hivIntake.label",
                "icon-asterisk",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/haiti/hiv/zl/hiv-adult-intake.xml&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),
                // ToDo: Add privileges and locations
                null,
                and(patientIsAdult())));

        extensions.add(visitAction(Extensions.HIV_ZL_ADULT_FOLLOWUP_VISIT_ACTION,
                "pih.task.hivFollowup.label",
                "icon-asterisk",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/haiti/hiv/zl/hiv-adult-followup.xml&returnUrl=/" + WebConstants.CONTEXT_PATH + "/" + patientVisitsPageUrl),
                // ToDo: Add privileges and locations
                null,
                and(patientIsAdult())));

        // iSantePlus forms
        extensions.add(visitAction(Extensions.HIV_ADULT_INITIAL_VISIT_ACTION,
                "pih.task.hivIntakeISantePlus.label",
                "icon-asterisk",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/haiti/hiv/iSantePlus/SaisiePremiereVisiteAdult.xml"),
                // ToDo: Add privileges and locations
                null,
                and(patientIsAdult())));

        extensions.add(visitAction(Extensions.HIV_PEDS_INITIAL_VISIT_ACTION,
                "pih.task.hivIntakeISantePlus.label",
                "icon-asterisk",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/haiti/hiv/iSantePlus/SaisiePremiereVisitePediatrique.xml"),
                // ToDo: Add privileges and locations
                null,
                and(patientIsChild())));

        extensions.add(visitAction(Extensions.HIV_ADULT_FOLLOWUP_VISIT_ACTION,
                "pih.task.hivFollowupISantePlus.label",
                "icon-asterisk",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/haiti/hiv/iSantePlus/VisiteDeSuivi.xml"),
                // ToDo: Add privileges and locations
                null,
                and(patientIsAdult())));

        extensions.add(visitAction(Extensions.HIV_PEDS_FOLLOWUP_VISIT_ACTION,
                "pih.task.hivFollowupISantePlus.label",
                "icon-asterisk",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/haiti/hiv/iSantePlus/VisiteDeSuiviPediatrique.xml"),
                // ToDo: Add privileges and locations
                null,
                and(patientIsChild())));

        extensions.add(visitAction(Extensions.HIV_ADHERENCE_VISIT_ACTION,
                "pih.task.hivAdherence.label",
                "icon-asterisk",
                "link",
                enterStandardHtmlFormLink("pihcore:htmlforms/haiti/hiv/iSantePlus/Adherence.xml"),
                // ToDo: Add privileges and locations
                null, null));

        addFeatureToggleToExtension(findExtensionById(Extensions.HIV_ZL_ADULT_INITIAL_VISIT_ACTION), "hiv");
        addFeatureToggleToExtension(findExtensionById(Extensions.HIV_ZL_ADULT_FOLLOWUP_VISIT_ACTION), "hiv");

        addFeatureToggleToExtension(findExtensionById(Extensions.HIV_ADULT_INITIAL_VISIT_ACTION), "hiv");
        addFeatureToggleToExtension(findExtensionById(Extensions.HIV_PEDS_INITIAL_VISIT_ACTION), "hiv");
        addFeatureToggleToExtension(findExtensionById(Extensions.HIV_ADULT_FOLLOWUP_VISIT_ACTION), "hiv");
        addFeatureToggleToExtension(findExtensionById(Extensions.HIV_PEDS_FOLLOWUP_VISIT_ACTION), "hiv");
        addFeatureToggleToExtension(findExtensionById(Extensions.HIV_ADHERENCE_VISIT_ACTION), "hiv");

        // HIV dashboard configuration
        apps.add(addToHivDashboardFirstColumn(app(Apps.HIV_PATIENT_PROGRAM_SUMMARY,
                "coreapps.currentEnrollmentDashboardWidget.label",
                "icon-stethoscope",  // TODO figure out right icon
                null,
                null, // TODO restrict by privilege or location)
                objectNode(
                        "widget", "programstatus",
                        "icon", "icon-stethoscope",
                        "label", "coreapps.currentEnrollmentDashboardWidget.label",
                        "dateFormat", "dd MMM yyyy",
                        "program", PihHaitiPrograms.HIV.uuid(),
                        "locationTag", LocationTags.VISIT_LOCATION.uuid()   // TODO what should this be
                )),
                "coreapps", "dashboardwidgets/dashboardWidget"));

        addFeatureToggleToApp(findAppById(Apps.HIV_PATIENT_PROGRAM_SUMMARY), "hiv");

        apps.add(addToHivDashboardSecondColumn(app(Apps.HIV_PATIENT_PROGRAM_HISTORY,
                "coreapps.programHistoryDashboardWidget.label",
                "icon-stethoscope",  // TODO figure out right icon
                null,
                null, // TODO restrict by privilege or location)
                objectNode(
                        "icon", "icon-stethoscope",
                        "label", "coreapps.programHistoryDashboardWidget.label",
                        "dateFormat", "dd MMM yyyy",
                        "program", PihHaitiPrograms.HIV.uuid(),
                        "includeActive", false,
                        "locationTag", LocationTags.VISIT_LOCATION.uuid()   // TODO what should this be
                )),
                "coreapps", "program/programHistory"));

        addFeatureToggleToApp(findAppById(Apps.HIV_PATIENT_PROGRAM_HISTORY), "hiv");

        // TODO correct the privilege
        apps.add(addToProgramSummaryListPage(app(Apps.HIV_PROGRAM_SUMMARY_DASHBOARD,
                "pih.app.hiv.programSummary.dashboard",
                "icon-list-alt",
                "/coreapps/summarydashboard/summaryDashboard.page?app=" + Apps.HIV_PROGRAM_SUMMARY_DASHBOARD,
                Privileges.APP_COREAPPS_SUMMARY_DASHBOARD.privilege(),
                null
                ),
                null));

        apps.add(addToHivSummaryDashboardFirstColumn(app(Apps.HIV_PROGRAM_SUMMARY,
                "Program Statistcs",
                "icon-stethoscope",  // TODO figure out right icon
                null,
                null, // TODO restrict by privilege or location)
                objectNode(
                        "widget", "programsummary",
                        "icon", "icon-stethoscope",
                        "label", "Program Statistcs",
                        "dateFormat", "dd MMM yyyy",
                        "program", PihHaitiPrograms.HIV.uuid()
                )),
                "coreapps", "dashboardwidgets/dashboardWidget"));
    }

    private void enableBiometrics(Config config) {

        extensions.add(fragmentExtension(Extensions.BIOMETRICS_FIND_PATIENT,
                "registrationapp",
                "biometrics/fingerprintSearch",
                null,   // shouldn't need a privilege, since this is injected into the patient search
                ExtensionPoints.PATIENT_SEARCH,
                map("scanUrl", config.getBiometricsConfig().getScanUrl(),
                        "devicesUrl", config.getBiometricsConfig().getDevicesUrl())));


        apps.add(addToRegistrationSummarySecondColumnContent(app(Apps.BIOMETRICS_SUMMARY,
                "registrationapp.biometrics.summary",
                "icon-user",
                null,
                "App: registrationapp.registerPatient",
                objectNode("registrationAppId", Apps.PATIENT_REGISTRATION)),
                "registrationapp",
                "summary/biometricsSummary"));


    }

    private void enableLabTracking() {

        apps.add(addToHomePage(app(Apps.LAB_TRACKING,
                "labtrackingapp.app.label",
                "icon-beaker",
                "/labtrackingapp/labtrackingViewQueue.page?appId=" + Apps.LAB_TRACKING,
                Privileges.APP_LAB_TRACKING_MONITOR_ORDERS.privilege(),
                null),
                null));  // TODO restrict by location ?

        extensions.add(visitAction(Extensions.ORDER_LAB_VISIT_ACTION,
                "labtrackingapp.orderPathology.label",
                "icon-beaker",
                "link",
                "/labtrackingapp/labtrackingAddOrder.page?patientId={{patient.uuid}}&visitId={{visit.id}}",
                null,
                null));     // TODO restrict by location ?

        apps.add(addToClinicianDashboardSecondColumn(app(Apps.LAB_SUMMARY,
                "labtrackingapp.pathology",
                "icon-beaker",
                null,
                null,  // TODO restrict by privilege or location?
                null),
                "labtrackingapp", "labtrackingPatientDashboard"));

    }

    private void enablePrograms(Config config, FeatureToggleProperties featureToggles) {

        List<String> supportedPrograms = new ArrayList<String>();

        if (config.isComponentEnabled(Components.HIV) && featureToggles.isFeatureEnabled("hiv")) {
            supportedPrograms.add(PihHaitiPrograms.HIV.uuid());
            enableHIV();
        }

        if (config.isComponentEnabled(Components.ZIKA) && featureToggles.isFeatureEnabled("zika")) {
            supportedPrograms.add(PihHaitiPrograms.ZIKA.uuid());
            enableZikaProgram();
        }

        // TODO better/more granular privileges?
        if (supportedPrograms.size() > 0) {

            apps.add(addToHomePage(app(Apps.PROGRAM_SUMMARY_LIST,
                    "pih.app.programSummaryList.title",
                    "icon-list-alt",
                    "/coreapps/applist/appList.page?app=" + Apps.PROGRAM_SUMMARY_LIST,
                    Privileges.APP_COREAPPS_SUMMARY_DASHBOARD.privilege(),
                    null),
                    null));

            apps.add(addToClinicianDashboardSecondColumn(app(Apps.PROGRAMS_LIST,
                    "coreapps.programsListDashboardWidget.label",
                    "icon-stethoscope",  // TODO figure out right icon
                    null,
                    Privileges.APP_COREAPPS_PATIENT_DASHBOARD.privilege(),
                    objectNode(
                            "widget", "programs",
                            "icon", "icon-stethoscope",
                            "label", "coreapps.programsDashboardWidget.label",
                            "dateFormat", "dd MMM yyyy",
                            "supportedPrograms", StringUtils.join(supportedPrograms, ','),
                            "enableProgramDashboards", "true"
                    )),
                    "coreapps", "dashboardwidgets/dashboardWidget"));
        }

        addFeatureToggleToApp(findAppById(Apps.PROGRAMS_LIST), "programsList");
    }

    private void enableZikaProgram() {

        // dashboard title configured via 3bea593a-9afd-4642-96a6-210b60f5aff2.custom.title=ZIKA PROGRAM message property

        apps.add(addToZikaDashboardFirstColumn(app(Apps.ZIKA_PATIENT_PROGRAM_SUMMARY,
                "coreapps.currentEnrollmentDashboardWidget.label",
                "icon-stethoscope",  // TODO figure out right icon
                null,
                Privileges.APP_COREAPPS_PATIENT_DASHBOARD.privilege(),
                objectNode(
                        "widget", "programstatus",
                        "icon", "icon-stethoscope",
                        "label", "coreapps.currentEnrollmentDashboardWidget.label",
                        "dateFormat", "dd MMM yyyy",
                        "program", PihHaitiPrograms.ZIKA.uuid(),
                        "locationTag", LocationTags.VISIT_LOCATION.uuid()   // TODO what should this be
                )),
                "coreapps", "dashboardwidgets/dashboardWidget"));

        addFeatureToggleToApp(findAppById(Apps.ZIKA_PATIENT_PROGRAM_SUMMARY), "zika");

        apps.add(addToZikaDashboardSecondColumn(app(Apps.ZIKA_PATIENT_PROGRAM_HISTORY,
                "coreapps.programHistoryDashboardWidget.label",
                "icon-stethoscope",  // TODO figure out right icon
                null,
                Privileges.APP_COREAPPS_PATIENT_DASHBOARD.privilege(),
                objectNode(
                        "icon", "icon-stethoscope",
                        "label", "coreapps.programHistoryDashboardWidget.label",
                        "dateFormat", "dd MMM yyyy",
                        "program", PihHaitiPrograms.ZIKA.uuid(),
                        "includeActive", false,
                        "locationTag", LocationTags.VISIT_LOCATION.uuid()   // TODO what should this be
                )),
                "coreapps", "program/programHistory"));

        addFeatureToggleToApp(findAppById(Apps.ZIKA_PATIENT_PROGRAM_HISTORY), "zika");
    }

    private void enableExportPatients() {
        apps.add(addToSystemAdministrationPage(app(Apps.PATIENT_EXPORT,
                "pihcore.patient.export",
                "icon-external-link",
                "pihcore/export/exportPatients.page",
                "App: emr.systemAdministration",
                null)));
    }

    private void enableImportPatients() {
        apps.add(addToSystemAdministrationPage(app(Apps.PATIENT_IMPORT,
                "pihcore.patient.import",
                "icon-signin",
                "pihcore/export/importPatients.page",
                "App: emr.systemAdministration",
                null)));
    }

    private void enableRelationships() {

        apps.add(addToClinicianDashboardSecondColumn(app(Apps.RELATIONSHIPS_CLINICAL_SUMMARY,
                "pihcore.relationshipsDashboardWidget.label",
                "icon-group",
                null,
                null, // TODO restrict by privilege or location)
                objectNode(
                        "widget", "relationships",
                        "editPrivilege", CoreAppsConstants.PRIVILEGE_EDIT_RELATIONSHIPS,
                        "dashboardPage", "/coreapps/clinicianfacing/patient.page?patientId={{patientUuid}}",
                        "providerPage", "/coreapps/providermanagement/editProvider.page?personUuid={{personUuid}}",
                        "includeRelationshipTypes", RelationshipTypeBundle.RelationshipTypes.SPOUSE_PARTNER
                                + "," + PihCoreConstants.RELATIONSHIP_SIBLING
                                + "," + PihCoreConstants.RELATIONSHIP_PARENT_CHILD,
                        "icon", "icon-group",
                        "label", "pihcore.relationshipsDashboardWidget.label"
                )),
                "coreapps", "dashboardwidgets/dashboardWidget"));

        addFeatureToggleToApp(findAppById(Apps.RELATIONSHIPS_CLINICAL_SUMMARY), "relationships");
    }

    private void enablePatientDocuments() {
        apps.add(addToClinicianDashboardSecondColumn(app(Apps.PATIENT_DOCUMENTS,
                "pihcore.patientDocuments.label",
                "icon-paper-clip",
                null,
                Privileges.TASK_EMR_ENTER_CONSULT_NOTE.privilege(),  // TODO: determine right privilege
                null),
                "visitdocumentsui", "dashboardWidget"));

        addFeatureToggleToApp(findAppById(Apps.PATIENT_DOCUMENTS), "patientDocuments");

        extensions.add(overallAction(Extensions.PATIENT_DOCUMENTS_OVERALL_ACTION,
                "pihcore.patientDocuments.overallAction.label",
                "icon-paper-clip",
                "link",
                "visitdocumentsui/visitDocuments.page?patient={{patient.uuid}}&patientId={{patient.patientId}}",
                Privileges.TASK_EMR_ENTER_CONSULT_NOTE.privilege(),  // TODO: determine right privilege
                null));

        addFeatureToggleToExtension(findExtensionById(Extensions.PATIENT_DOCUMENTS_OVERALL_ACTION), "patientDocuments");
    }


    private void registerLacollinePatientRegistrationEncounterTypes() {
        // TODO: I *believe* these are used in Lacolline, but not 100% sure
        registerTemplateForEncounterType(EncounterTypes.PAYMENT,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-money");
        registerTemplateForEncounterType(EncounterTypes.PRIMARY_CARE_VISIT,
                findExtensionById(EncounterTemplates.DEFAULT), "icon-calendar");

    }

    private void addPaperRecordActionsIncludesIfNeeded() {

        // this provides the javascript the backs the three overall action buttons
        // we need to make sure we don't add it twice
        if (! containsExtension(extensions, Extensions.PAPER_RECORD_ACTIONS_INCLUDES)) {
            extensions.add(fragmentExtension(Extensions.PAPER_RECORD_ACTIONS_INCLUDES,
                    "paperrecord",
                    "patientdashboard/overallActionsIncludes",
                    null,
                    ExtensionPoints.DASHBOARD_INCLUDE_FRAGMENTS,
                    null));
        }
    }

    public AppDescriptor findAppById(String id) {
        for (AppDescriptor app : apps) {
            if (app.getId().equals(id)) {
                return app;
            }
        }
        log.warn("App Not Found: " + id);
        return null;
    }

    public Extension findExtensionById(String id) {
        for (Extension extension : extensions) {
            if (extension.getId().equals(id)) {
                return extension;
            }
        }
        log.warn("Extension Not Found: " + id);
        return null;
    }

    public void setReadyForRefresh(Boolean readyForRefresh) {
        this.readyForRefresh = readyForRefresh;
    }

    // used for mocking
    public void setApps(List<AppDescriptor> apps) {
        this.apps = apps;
    }

    public void setExtensions(List<Extension> extensions) {
        this.extensions = extensions;
    }

    public void setConfig(Config config) {
        this.config = config;
    }
}


