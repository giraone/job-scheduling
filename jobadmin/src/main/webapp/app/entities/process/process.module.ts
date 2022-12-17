import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { ProcessComponent } from './list/process.component';
import { ProcessDetailComponent } from './detail/process-detail.component';
import { ProcessUpdateComponent } from './update/process-update.component';
import { ProcessDeleteDialogComponent } from './delete/process-delete-dialog.component';
import { ProcessRoutingModule } from './route/process-routing.module';

@NgModule({
  imports: [SharedModule, ProcessRoutingModule],
  declarations: [ProcessComponent, ProcessDetailComponent, ProcessUpdateComponent, ProcessDeleteDialogComponent],
})
export class ProcessModule {}
