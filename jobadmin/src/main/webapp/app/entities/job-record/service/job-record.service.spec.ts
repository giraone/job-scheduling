import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IJobRecord } from '../job-record.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../job-record.test-samples';

import { JobRecordService, RestJobRecord } from './job-record.service';

const requireRestSample: RestJobRecord = {
  ...sampleWithRequiredData,
  jobAcceptedTimestamp: sampleWithRequiredData.jobAcceptedTimestamp?.toJSON(),
  lastEventTimestamp: sampleWithRequiredData.lastEventTimestamp?.toJSON(),
  lastRecordUpdateTimestamp: sampleWithRequiredData.lastRecordUpdateTimestamp?.toJSON(),
};

describe('JobRecord Service', () => {
  let service: JobRecordService;
  let httpMock: HttpTestingController;
  let expectedResult: IJobRecord | IJobRecord[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(JobRecordService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a JobRecord', () => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const jobRecord = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(jobRecord).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a JobRecord', () => {
      const jobRecord = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(jobRecord).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a JobRecord', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of JobRecord', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a JobRecord', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addJobRecordToCollectionIfMissing', () => {
      it('should add a JobRecord to an empty array', () => {
        const jobRecord: IJobRecord = sampleWithRequiredData;
        expectedResult = service.addJobRecordToCollectionIfMissing([], jobRecord);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(jobRecord);
      });

      it('should not add a JobRecord to an array that contains it', () => {
        const jobRecord: IJobRecord = sampleWithRequiredData;
        const jobRecordCollection: IJobRecord[] = [
          {
            ...jobRecord,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, jobRecord);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a JobRecord to an array that doesn't contain it", () => {
        const jobRecord: IJobRecord = sampleWithRequiredData;
        const jobRecordCollection: IJobRecord[] = [sampleWithPartialData];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, jobRecord);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(jobRecord);
      });

      it('should add only unique JobRecord to an array', () => {
        const jobRecordArray: IJobRecord[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const jobRecordCollection: IJobRecord[] = [sampleWithRequiredData];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, ...jobRecordArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const jobRecord: IJobRecord = sampleWithRequiredData;
        const jobRecord2: IJobRecord = sampleWithPartialData;
        expectedResult = service.addJobRecordToCollectionIfMissing([], jobRecord, jobRecord2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(jobRecord);
        expect(expectedResult).toContain(jobRecord2);
      });

      it('should accept null and undefined values', () => {
        const jobRecord: IJobRecord = sampleWithRequiredData;
        expectedResult = service.addJobRecordToCollectionIfMissing([], null, jobRecord, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(jobRecord);
      });

      it('should return initial array if no JobRecord is added', () => {
        const jobRecordCollection: IJobRecord[] = [sampleWithRequiredData];
        expectedResult = service.addJobRecordToCollectionIfMissing(jobRecordCollection, undefined, null);
        expect(expectedResult).toEqual(jobRecordCollection);
      });
    });

    describe('compareJobRecord', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareJobRecord(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareJobRecord(entity1, entity2);
        const compareResult2 = service.compareJobRecord(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareJobRecord(entity1, entity2);
        const compareResult2 = service.compareJobRecord(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareJobRecord(entity1, entity2);
        const compareResult2 = service.compareJobRecord(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
