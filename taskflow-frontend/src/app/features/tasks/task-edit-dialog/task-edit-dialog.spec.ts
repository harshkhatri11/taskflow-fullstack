import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskEditDialog } from './task-edit-dialog';

describe('TaskEditDialog', () => {
  let component: TaskEditDialog;
  let fixture: ComponentFixture<TaskEditDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskEditDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskEditDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
