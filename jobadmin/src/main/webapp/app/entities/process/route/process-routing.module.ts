import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ProcessComponent } from '../list/process.component';
import { ProcessDetailComponent } from '../detail/process-detail.component';
import { ProcessUpdateComponent } from '../update/process-update.component';
import { ProcessRoutingResolveService } from './process-routing-resolve.service';
import { ASC } from 'app/config/navigation.constants';

const processRoute: Routes = [
  {
    path: '',
    component: ProcessComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ProcessDetailComponent,
    resolve: {
      process: ProcessRoutingResolveService,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ProcessUpdateComponent,
    resolve: {
      process: ProcessRoutingResolveService,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ProcessUpdateComponent,
    resolve: {
      process: ProcessRoutingResolveService,
    },
    canActivate: [UserRouteAccessService],
  },
];

@NgModule({
  imports: [RouterModule.forChild(processRoute)],
  exports: [RouterModule],
})
export class ProcessRoutingModule {}
