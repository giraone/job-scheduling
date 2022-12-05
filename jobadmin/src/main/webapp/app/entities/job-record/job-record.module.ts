import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { JobRecordComponent } from './list/job-record.component';
import { JobRecordDetailComponent } from './detail/job-record-detail.component';
import { JobRecordUpdateComponent } from './update/job-record-update.component';
import { JobRecordDeleteDialogComponent } from './delete/job-record-delete-dialog.component';
import { JobRecordRoutingModule } from './route/job-record-routing.module';

@NgModule({
  imports: [SharedModule, JobRecordRoutingModule],
  declarations: [JobRecordComponent, JobRecordDetailComponent, JobRecordUpdateComponent, JobRecordDeleteDialogComponent],
  entryComponents: [JobRecordDeleteDialogComponent],
})
export class JobRecordModule {}
