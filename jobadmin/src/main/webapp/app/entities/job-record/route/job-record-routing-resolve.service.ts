import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IJobRecord, JobRecord } from '../job-record.model';
import { JobRecordService } from '../service/job-record.service';

@Injectable({ providedIn: 'root' })
export class JobRecordRoutingResolveService implements Resolve<IJobRecord> {
  constructor(protected service: JobRecordService, protected router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IJobRecord> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((jobRecord: HttpResponse<JobRecord>) => {
          if (jobRecord.body) {
            return of(jobRecord.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new JobRecord());
  }
}
