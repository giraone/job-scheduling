import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'job-record',
        data: { pageTitle: 'jobadminApp.jobRecord.home.title' },
        loadChildren: () => import('./job-record/job-record.module').then(m => m.JobRecordModule),
      },
      {
        path: 'process',
        data: { pageTitle: 'jobadminApp.process.home.title' },
        loadChildren: () => import('./process/process.module').then(m => m.ProcessModule),
      },
      /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
    ]),
  ],
})
export class EntityRoutingModule {}
