import { Component, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, map } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { IJobRecord } from '../job-record.model';

import { ASC, DESC, ITEMS_PER_PAGE, SORT } from '../../../config/pagination.constants';
import { IProcess } from '../../../entities/process/process.model';
import { ProcessService } from '../../../entities/process/service/process.service';
import { JobStatusEnum } from '../../../entities/enumerations/job-status-enum.model';
import { JobRecordDeleteDialogComponent } from '../delete/job-record-delete-dialog.component';
import { JobRecordService } from '../service/job-record.service';

@Component({
  selector: 'jhi-job-record',
  templateUrl: './job-record.component.html',
})
export class JobRecordComponent implements OnInit {
  jobRecords?: IJobRecord[];
  isLoading = false;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page?: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;

  // adapted
  jobStatusEnumValues = Object.keys(JobStatusEnum);
  processesSharedCollection: IProcess[] = [];
  statusFilter: string | null = null;
  processFilter: string | null = null;

  constructor(
    protected jobRecordService: JobRecordService,
    protected processService: ProcessService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal
  ) {}

  loadPage(page?: number, dontNavigate?: boolean): void {
    this.isLoading = true;
    const pageToLoad: number = page ?? this.page ?? 1;

    const params: any = {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      sort: this.sort(),
    };
    if (this.statusFilter != null) {
      params.status = this.statusFilter;
    }
    if (this.processFilter != null) {
      params.process = this.processFilter;
    }
    this.jobRecordService.query(params).subscribe({
      next: (res: HttpResponse<IJobRecord[]>) => {
        this.isLoading = false;
        this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate);
      },
      error: () => {
        this.isLoading = false;
        this.onError();
      },
    });
  }

  deleteAll(): void {
    this.isLoading = true;
    this.jobRecordService.deleteAll().subscribe({
      next: () => {
        this.isLoading = false;
        this.loadPage(1);
      },
      error: () => {
        this.isLoading = false;
        this.onError();
      },
    });
  }

  ngOnInit(): void {
    this.handleNavigation();
    // adapted
    this.loadRelationshipsOptions();
  }

  trackId(_index: number, item: IJobRecord): number {
    return item.id!;
  }

  delete(jobRecord: IJobRecord): void {
    const modalRef = this.modalService.open(JobRecordDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.jobRecord = jobRecord;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadPage();
      }
    });
  }

  protected sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? ASC : DESC)];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  // adapted
  protected loadRelationshipsOptions(): void {
    this.processService
      .query()
      .pipe(map((res: HttpResponse<IProcess[]>) => res.body ?? []))
      .pipe(map((processes: IProcess[]) => this.processService.addProcessToCollectionIfMissing(processes, null)))
      .subscribe((processes: IProcess[]) => (this.processesSharedCollection = processes));
  }

  protected handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap]).subscribe(([data, params]) => {
      this.statusFilter = params.get('status');
      this.processFilter = params.get('process');
      const page = params.get('page');
      const pageNumber = +(page ?? 1);
      const sort = (params.get(SORT) ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === ASC;
      if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        this.predicate = predicate;
        this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    });
  }

  protected onSuccess(data: IJobRecord[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/job-record'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? ASC : DESC),
        },
      });
    }
    this.jobRecords = data ?? [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
