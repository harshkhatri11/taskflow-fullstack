import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectViewDialog } from './project-view-dialog';

describe('ProjectViewDialog', () => {
  let component: ProjectViewDialog;
  let fixture: ComponentFixture<ProjectViewDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectViewDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectViewDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
