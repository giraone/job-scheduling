import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IProcess } from '../process.model';
import { ProcessService } from '../service/process.service';

@Injectable({ providedIn: 'root' })
export class ProcessRoutingResolveService implements Resolve<IProcess | null> {
  constructor(protected service: ProcessService, protected router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IProcess | null | never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((process: HttpResponse<IProcess>) => {
          if (process.body) {
            return of(process.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(null);
  }
}
