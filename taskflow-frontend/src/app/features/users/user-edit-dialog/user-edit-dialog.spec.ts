import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserEditDialog } from './user-edit-dialog';

describe('UserEditDialog', () => {
  let component: UserEditDialog;
  let fixture: ComponentFixture<UserEditDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserEditDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(UserEditDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
