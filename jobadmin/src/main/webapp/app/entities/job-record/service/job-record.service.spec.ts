import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { JobStatusEnum } from 'app/entities/enumerations/job-status-enum.model';
import { IJobRecord, JobRecord } from '../job-record.model';

import { JobRecordService } from './job-record.service';

describe('JobRecord Service', () => {
  let service: JobRecordService;
  let httpMock: HttpTestingController;
  let elemDefault: IJobRecord;
  let expectedResult: IJobRecord | IJobRecord[] | boolean | null;
  let currentDate: dayjs.Dayjs;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(JobRecordService);
    httpMock = TestBed.inject(HttpTestingController);
    currentDate = dayjs();

    elemDefault = {
      id: 0,
      jobAcceptedTimestamp: currentDate,
      lastEventTimestamp: currentDate,
      lastRecordUpdateTimestamp: currentDate,
      status: JobStatusEnum.ACCEPTED,
      pausedBucketKey: 'AAAAAAA',
    };
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = Object.assign(
        {
          jobAcceptedTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastEventTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastRecordUpdateTimestamp: currentDate.format(DATE_TIME_FORMAT),
        },
        elemDefault
      );

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(elemDefault);
    });

    it('should create a JobRecord', () => {
      const returnedFromService = Object.assign(
        {
          id: 0,
          jobAcceptedTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastEventTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastRecordUpdateTimestamp: currentDate.format(DATE_TIME_FORMAT),
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          jobAcceptedTimestamp: currentDate,
          lastEventTimestamp: currentDate,
          lastRecordUpdateTimestamp: currentDate,
        },
        returnedFromService
      );

      service.create(new JobRecord()).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a JobRecord', () => {
      const returnedFromService = Object.assign(
        {
          id: 1,
          jobAcceptedTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastEventTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastRecordUpdateTimestamp: currentDate.format(DATE_TIME_FORMAT),
          status: 'BBBBBB',
          pausedBucketKey: 'BBBBBB',
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          jobAcceptedTimestamp: currentDate,
          lastEventTimestamp: currentDate,
          lastRecordUpdateTimestamp: currentDate,
        },
        returnedFromService
      );

      service.update(expected).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a JobRecord', () => {
      const patchObject = Object.assign(
        {
          lastEventTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastRecordUpdateTimestamp: currentDate.format(DATE_TIME_FORMAT),
          pausedBucketKey: 'BBBBBB',
        },
        new JobRecord()
      );

      const returnedFromService = Object.assign(patchObject, elemDefault);

      const expected = Object.assign(
        {
          jobAcceptedTimestamp: currentDate,
          lastEventTimestamp: currentDate,
          lastRecordUpdateTimestamp: currentDate,
        },
        returnedFromService
      );

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of JobRecord', () => {
      const returnedFromService = Object.assign(
        {
          id: 1,
          jobAcceptedTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastEventTimestamp: currentDate.format(DATE_TIME_FORMAT),
          lastRecordUpdateTimestamp: currentDate.format(DATE_TIME_FORMAT),
          status: 'BBBBBB',
          pausedBucketKey: 'BBBBBB',
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          jobAcceptedTimestamp: currentDate,
          lastEventTimestamp: currentDate,
          lastRecordUpdateTimestamp: currentDate,
        },
        returnedFromService
      );

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toContainEqual(expected);
    });

    it('should delete a JobRecord', () => {
      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult);
    });

    describe('addJobRecordToCollectionIfMissing', () => {
      it('should add a JobRecord to an empty array', () => {
        const jobRecord: IJobRecord = { id: 123 };
        expectedResult = service.addJobRecordToCollectionIfMissing([], jobRecord);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(jobRecord);
      });

      it('should not add a JobRecord to an array that contains it', () => {
        const jobRecord: IJobRecord = { id: 123 };
        const jobRecordCollection: IJobRecord[] = [
          {
            ...jobRecord,
          },
          { id: 456 },
        ];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, jobRecord);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a JobRecord to an array that doesn't contain it", () => {
        const jobRecord: IJobRecord = { id: 123 };
        const jobRecordCollection: IJobRecord[] = [{ id: 456 }];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, jobRecord);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(jobRecord);
      });

      it('should add only unique JobRecord to an array', () => {
        const jobRecordArray: IJobRecord[] = [{ id: 123 }, { id: 456 }, { id: 17919 }];
        const jobRecordCollection: IJobRecord[] = [{ id: 123 }];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, ...jobRecordArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const jobRecord: IJobRecord = { id: 123 };
        const jobRecord2: IJobRecord = { id: 456 };
        expectedResult = service.addJobRecordToCollectionIfMissing([], jobRecord, jobRecord2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(jobRecord);
        expect(expectedResult).toContain(jobRecord2);
      });

      it('should accept null and undefined values', () => {
        const jobRecord: IJobRecord = { id: 123 };
        expectedResult = service.addJobRecordToCollectionIfMissing([], null, jobRecord, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(jobRecord);
      });

      it('should return initial array if no JobRecord is added', () => {
        const jobRecordCollection: IJobRecord[] = [{ id: 123 }];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, undefined, null);
        expect(expectedResult).toEqual(jobRecordCollection);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
