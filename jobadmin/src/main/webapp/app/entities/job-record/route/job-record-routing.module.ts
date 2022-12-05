import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { JobRecordComponent } from '../list/job-record.component';
import { JobRecordDetailComponent } from '../detail/job-record-detail.component';
import { JobRecordUpdateComponent } from '../update/job-record-update.component';
import { JobRecordRoutingResolveService } from './job-record-routing-resolve.service';

const jobRecordRoute: Routes = [
  {
    path: '',
    component: JobRecordComponent,
    data: {
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: JobRecordDetailComponent,
    resolve: {
      jobRecord: JobRecordRoutingResolveService,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: JobRecordUpdateComponent,
    resolve: {
      jobRecord: JobRecordRoutingResolveService,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: JobRecordUpdateComponent,
    resolve: {
      jobRecord: JobRecordRoutingResolveService,
    },
    canActivate: [UserRouteAccessService],
  },
];

@NgModule({
  imports: [RouterModule.forChild(jobRecordRoute)],
  exports: [RouterModule],
})
export class JobRecordRoutingModule {}
