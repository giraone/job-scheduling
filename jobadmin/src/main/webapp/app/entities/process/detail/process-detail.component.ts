import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IProcess } from '../process.model';

@Component({
  selector: 'jhi-process-detail',
  templateUrl: './process-detail.component.html',
})
export class ProcessDetailComponent implements OnInit {
  process: IProcess | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ process }) => {
      this.process = process;
    });
  }

  previousState(): void {
    window.history.back();
  }
}
