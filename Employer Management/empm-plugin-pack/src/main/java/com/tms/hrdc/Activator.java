package com.tms.hrdc;

import com.tms.hrdc.binder.*;
import com.tms.hrdc.webservice.EmpmAPI;
import com.tms.hrdc.datalistAction.*;
import com.tms.hrdc.defaultPluginTool.*;
import com.tms.hrdc.element.AuditTrailElement_;
import com.tms.hrdc.element.FileUploadVideoPreview;
import com.tms.hrdc.formGridBinder.EngagementChangeListBinder;
import com.tms.hrdc.validator.EmpMyCoIDValidator;
import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here      
        registrationList.add(context.registerService(EmpmAPI.class.getName(), new EmpmAPI(), null));
        registrationList.add(context.registerService(QueryBinder.class.getName(), new QueryBinder(), null));        
        registrationList.add(context.registerService(SendEmailTool.class.getName(), new SendEmailTool(), null));
        registrationList.add(context.registerService(ArchiveBinder.class.getName(), new ArchiveBinder(), null));
        registrationList.add(context.registerService(CriteriaChecker.class.getName(), new CriteriaChecker(), null));
//        registrationList.add(context.registerService(SentEmailFromForm.class.getName(), new SentEmailFromForm(), null));
        registrationList.add(context.registerService(PotEmpExcelBinder.class.getName(), new PotEmpExcelBinder(), null));
        registrationList.add(context.registerService(DeleteUserAndRecord.class.getName(), new DeleteUserAndRecord(), null));        
        registrationList.add(context.registerService(PotEmpMailListButton.class.getName(), new PotEmpMailListButton(), null));
        registrationList.add(context.registerService(PotEmpEmailBinder.class.getName(), new PotEmpEmailBinder(), null));
        registrationList.add(context.registerService(SchedulerPotentialEmployer.class.getName(), new SchedulerPotentialEmployer(), null));
        registrationList.add(context.registerService(SchedulerNotification.class.getName(), new SchedulerNotification(), null));
        registrationList.add(context.registerService(PotEmpStateChangerButton.class.getName(), new PotEmpStateChangerButton(), null));
        registrationList.add(context.registerService(AccountCreatorBinder.class.getName(), new AccountCreatorBinder(), null));
        registrationList.add(context.registerService(AccountCreatorTool.class.getName(), new AccountCreatorTool(), null));
        registrationList.add(context.registerService(EmailTemplateTool.class.getName(), new EmailTemplateTool(), null));
        registrationList.add(context.registerService(TaskAssignerTool.class.getName(), new TaskAssignerTool(), null));        
        registrationList.add(context.registerService(RefNumGenerator.class.getName(), new RefNumGenerator(), null));
        registrationList.add(context.registerService(QueryReminder.class.getName(), new QueryReminder(), null));
        registrationList.add(context.registerService(TnCFileParser.class.getName(), new TnCFileParser(), null));
        registrationList.add(context.registerService(BranchBinder.class.getName(), new BranchBinder(), null));
        registrationList.add(context.registerService(AuditTrailElement_.class.getName(), new AuditTrailElement_(), null));
        registrationList.add(context.registerService(AuditTrailTool.class.getName(), new AuditTrailTool(), null));
        registrationList.add(context.registerService(EmployerStatusUpdate.class.getName(), new EmployerStatusUpdate(), null));
        registrationList.add(context.registerService(DraftSavesBinder.class.getName(), new DraftSavesBinder(), null));
        registrationList.add(context.registerService(ApproverBinder.class.getName(), new ApproverBinder(), null));
        registrationList.add(context.registerService(EmpMyCoIDValidator.class.getName(), new EmpMyCoIDValidator(), null));
        registrationList.add(context.registerService(PotEmpFlowBinder.class.getName(), new PotEmpFlowBinder(), null));
        registrationList.add(context.registerService(AssignmentManager.class.getName(), new AssignmentManager(), null));
        registrationList.add(context.registerService(ReturnTaskButton.class.getName(), new ReturnTaskButton(), null));
        registrationList.add(context.registerService(DeregisterTool.class.getName(), new DeregisterTool(), null));
        registrationList.add(context.registerService(EngagementButton.class.getName(), new EngagementButton(), null));
        registrationList.add(context.registerService(EnforcementButton.class.getName(), new EnforcementButton(), null));
        registrationList.add(context.registerService(EngagementChangeListBinder.class.getName(), new EngagementChangeListBinder(), null));
        registrationList.add(context.registerService(ChangesMergerTool.class.getName(), new ChangesMergerTool(), null));
        registrationList.add(context.registerService(RegisterEmpAPS.class.getName(), new RegisterEmpAPS(), null));
        registrationList.add(context.registerService(FormManagerTool.class.getName(), new FormManagerTool(), null));
        registrationList.add(context.registerService(DeregistrationWithdrawalTool.class.getName(), new DeregistrationWithdrawalTool(), null));
        registrationList.add(context.registerService(DeregistrationCancellationTool.class.getName(), new DeregistrationCancellationTool(), null));
        registrationList.add(context.registerService(EnforcementBinder.class.getName(), new EnforcementBinder(), null));
        registrationList.add(context.registerService(EmpFlowBinder.class.getName(), new EmpFlowBinder(), null));
        registrationList.add(context.registerService(ForwardReturnTool.class.getName(), new ForwardReturnTool(), null));
        registrationList.add(context.registerService(SchedularTemporaryLogin.class.getName(), new SchedularTemporaryLogin(), null));
        registrationList.add(context.registerService(PotEmpMailListButton.class.getName(), new PotEmpMailListButton(), null));
        registrationList.add(context.registerService(CKSPBinder.class.getName(), new CKSPBinder(), null));
        registrationList.add(context.registerService(ChangePEStatus.class.getName(), new ChangePEStatus(),null));
        registrationList.add(context.registerService(DisbursementBinder.class.getName(), new DisbursementBinder(), null));
        registrationList.add(context.registerService(AbortGeneralProcesses.class.getName(), new AbortGeneralProcesses(),null));
        registrationList.add(context.registerService(DeregNotifyGrantOfficerTool.class.getName(), new DeregNotifyGrantOfficerTool(),null));
        registrationList.add(context.registerService(EmpmEnvBinder.class.getName(), new EmpmEnvBinder(),null));
        registrationList.add(context.registerService(QueryCleaner.class.getName(), new QueryCleaner(),null));
        registrationList.add(context.registerService(EmailResendBinder.class.getName(), new EmailResendBinder(),null));
        registrationList.add(context.registerService(EmployerProfileSaveBinder.class.getName(), new EmployerProfileSaveBinder(),null));
        registrationList.add(context.registerService(ImportPotentialEmpScheduler.class.getName(), new ImportPotentialEmpScheduler(),null));
        registrationList.add(context.registerService(DeleteSavedApplicationButton.class.getName(), new DeleteSavedApplicationButton(),null));
        registrationList.add(context.registerService(SchedulerArchiveDataImport.class.getName(), new SchedulerArchiveDataImport(),null));
        registrationList.add(context.registerService(MigrationTool.class.getName(), new MigrationTool(),null));
        registrationList.add(context.registerService(FileUploadVideoPreview.class.getName(), new FileUploadVideoPreview(),null));

    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}