import { ActivationEnum } from 'app/entities/enumerations/activation-enum.model';

import { IProcess, NewProcess } from './process.model';

export const sampleWithRequiredData: IProcess = {
  id: 67972,
  key: 'Paradigm RAM invoice',
  name: 'brand',
  activation: ActivationEnum['PAUSED'],
};

export const sampleWithPartialData: IProcess = {
  id: 24208,
  key: 'copying Mouse',
  name: 'Shoes red',
  activation: ActivationEnum['PAUSED'],
  agentKey: 'SQL',
};

export const sampleWithFullData: IProcess = {
  id: 60659,
  key: 'Expanded',
  name: 'Intelligent Security',
  activation: ActivationEnum['ACTIVE'],
  agentKey: 'Polarised',
  bucketKeyIfPaused: 'Open-source',
};

export const sampleWithNewData: NewProcess = {
  key: 'haptic Money',
  name: 'bandwidth Fantastic Orchestrator',
  activation: ActivationEnum['ACTIVE'],
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
