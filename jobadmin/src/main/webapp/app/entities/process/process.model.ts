import { ActivationEnum } from 'app/entities/enumerations/activation-enum.model';

export interface IProcess {
  id?: string;
  name?: string;
  activation?: ActivationEnum;
}

export class Process implements IProcess {
  constructor(public id?: string, public name?: string, public activation?: ActivationEnum) {}
}

export function getProcessIdentifier(process: IProcess): string | undefined {
  return process.id;
}
