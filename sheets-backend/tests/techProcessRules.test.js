import { describe, expect, it } from 'vitest';
import {
  isValidCatalogType_,
  nextOperationOrder_,
  nextSetupOrder_,
  planTechProcessCascadeDelete_,
  setupOrderLabel_,
  validateCatalogItemType_,
  validateReorderIds_,
} from '../pure/techProcessRules.js';

describe('setupOrderLabel_', () => {
  it('maps 0..9 to Roman I..X', () => {
    expect(setupOrderLabel_(0)).toBe('I');
    expect(setupOrderLabel_(1)).toBe('II');
    expect(setupOrderLabel_(9)).toBe('X');
  });

  it('falls back past X', () => {
    expect(setupOrderLabel_(10)).toBe('11');
  });
});

describe('catalog types', () => {
  it('accepts tool/plate/jaw', () => {
    expect(isValidCatalogType_('tool')).toBe(true);
    expect(isValidCatalogType_('plate')).toBe(true);
    expect(isValidCatalogType_('jaw')).toBe(true);
    expect(isValidCatalogType_('other')).toBe(false);
  });

  it('validates operation ref types', () => {
    expect(validateCatalogItemType_('tool', 'tool', 'tool_id').ok).toBe(true);
    expect(validateCatalogItemType_('plate', 'tool', 'tool_id').ok).toBe(false);
  });
});

describe('validateReorderIds_', () => {
  it('accepts permutation and builds order map', () => {
    const result = validateReorderIds_([3, 1, 2], [1, 2, 3]);
    expect(result.ok).toBe(true);
    expect(result.orderById).toEqual({ 3: 0, 1: 1, 2: 2 });
  });

  it('rejects missing or duplicate ids', () => {
    expect(validateReorderIds_([1, 2], [1, 2, 3]).ok).toBe(false);
    expect(validateReorderIds_([1, 1, 2], [1, 2, 3]).ok).toBe(false);
    expect(validateReorderIds_(null, [1]).ok).toBe(false);
  });
});

describe('planTechProcessCascadeDelete_', () => {
  it('orders deletes ops → setups → tp by descending __row', () => {
    const plan = planTechProcessCascadeDelete_(
      { id: 10, __row: 2 },
      [
        { id: 1, __row: 5 },
        { id: 2, __row: 8 },
      ],
      [
        { id: 100, setup_id: 1, __row: 12 },
        { id: 101, setup_id: 2, __row: 9 },
      ],
    );
    expect(plan.operations.map((r) => r.__row)).toEqual([12, 9]);
    expect(plan.setups.map((r) => r.__row)).toEqual([8, 5]);
    expect(plan.techProcess.id).toBe(10);
  });
});

describe('next order helpers', () => {
  it('computes next setup/operation order', () => {
    expect(nextSetupOrder_([])).toBe(0);
    expect(nextSetupOrder_([{ order: 0 }, { order: 2 }])).toBe(3);
    expect(nextOperationOrder_([{ order: 1 }])).toBe(2);
  });
});
