import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormPulse } from './form-pulse';

describe('FormPulse', () => {
  let component: FormPulse;
  let fixture: ComponentFixture<FormPulse>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormPulse],
    }).compileComponents();

    fixture = TestBed.createComponent(FormPulse);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
