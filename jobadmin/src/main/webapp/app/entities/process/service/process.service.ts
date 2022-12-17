import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IProcess, NewProcess } from '../process.model';

export type PartialUpdateProcess = Partial<IProcess> & Pick<IProcess, 'id'>;

export type EntityResponseType = HttpResponse<IProcess>;
export type EntityArrayResponseType = HttpResponse<IProcess[]>;

@Injectable({ providedIn: 'root' })
export class ProcessService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/processes');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(process: NewProcess): Observable<EntityResponseType> {
    return this.http.post<IProcess>(this.resourceUrl, process, { observe: 'response' });
  }

  update(process: IProcess): Observable<EntityResponseType> {
    return this.http.put<IProcess>(`${this.resourceUrl}/${this.getProcessIdentifier(process)}`, process, { observe: 'response' });
  }

  partialUpdate(process: PartialUpdateProcess): Observable<EntityResponseType> {
    return this.http.patch<IProcess>(`${this.resourceUrl}/${this.getProcessIdentifier(process)}`, process, { observe: 'response' });
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

  getProcessIdentifier(process: Pick<IProcess, 'id'>): number {
    return process.id;
  }

  compareProcess(o1: Pick<IProcess, 'id'> | null, o2: Pick<IProcess, 'id'> | null): boolean {
    return o1 && o2 ? this.getProcessIdentifier(o1) === this.getProcessIdentifier(o2) : o1 === o2;
  }

  addProcessToCollectionIfMissing<Type extends Pick<IProcess, 'id'>>(
    processCollection: Type[],
    ...processesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const processes: Type[] = processesToCheck.filter(isPresent);
    if (processes.length > 0) {
      const processCollectionIdentifiers = processCollection.map(processItem => this.getProcessIdentifier(processItem)!);
      const processesToAdd = processes.filter(processItem => {
        const processIdentifier = this.getProcessIdentifier(processItem);
        if (processCollectionIdentifiers.includes(processIdentifier)) {
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
