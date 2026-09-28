import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskViewDialog } from './task-view-dialog';

describe('TaskViewDialog', () => {
  let component: TaskViewDialog;
  let fixture: ComponentFixture<TaskViewDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskViewDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskViewDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
