import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IProcess, getProcessIdentifier } from '../process.model';

export type EntityResponseType = HttpResponse<IProcess>;
export type EntityArrayResponseType = HttpResponse<IProcess[]>;

@Injectable({ providedIn: 'root' })
export class ProcessService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/processes');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(process: IProcess): Observable<EntityResponseType> {
    return this.http.post<IProcess>(this.resourceUrl, process, { observe: 'response' });
  }

  update(process: IProcess): Observable<EntityResponseType> {
    return this.http.put<IProcess>(`${this.resourceUrl}/${getProcessIdentifier(process) as number}`, process, { observe: 'response' });
  }

  partialUpdate(process: IProcess): Observable<EntityResponseType> {
    return this.http.patch<IProcess>(`${this.resourceUrl}/${getProcessIdentifier(process) as number}`, process, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IProcess>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IProcess[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  addProcessToCollectionIfMissing(processCollection: IProcess[], ...processesToCheck: (IProcess | null | undefined)[]): IProcess[] {
    const processes: IProcess[] = processesToCheck.filter(isPresent);
    if (processes.length > 0) {
      const processCollectionIdentifiers = processCollection.map(processItem => getProcessIdentifier(processItem)!);
      const processesToAdd = processes.filter(processItem => {
        const processIdentifier = getProcessIdentifier(processItem);
        if (processIdentifier == null || processCollectionIdentifiers.includes(processIdentifier)) {
          return false;
        }
        processCollectionIdentifiers.push(processIdentifier);
        return true;
      });
      return [...processesToAdd, ...processCollection];
    }
    return processCollection;
  }
}
