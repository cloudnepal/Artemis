import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ArtemisTestModule } from '../../test.module';
import { Theme, ThemeService } from 'app/core/theme/theme.service';
import { EmojiPickerComponent } from 'app/shared/metis/emoji/emoji-picker.component';
import { PickerComponent } from '@ctrl/ngx-emoji-mart';
import { TranslatePipeMock } from '../../helpers/mocks/service/mock-translate.service';
import { MockComponent } from 'ng-mocks';
import { EmojiData } from '@ctrl/ngx-emoji-mart/ngx-emoji';

describe('EmojiPickerComponent', () => {
    let fixture: ComponentFixture<EmojiPickerComponent>;
    let comp: EmojiPickerComponent;
    let mockThemeService: ThemeService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule],
            declarations: [TranslatePipeMock, EmojiPickerComponent, MockComponent(PickerComponent)],
        })
            .compileComponents()
            .then(() => {
                mockThemeService = TestBed.inject(ThemeService);
                fixture = TestBed.createComponent(EmojiPickerComponent);
                comp = fixture.componentInstance;
            });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should react to theme changes', () => {
        expect(comp.dark()).toBeFalse();
        expect(comp.singleImageFunction()({ unified: '1F519' } as EmojiData)).toBe('');

        mockThemeService.applyThemePreference(Theme.DARK);

        expect(comp.dark()).toBeTrue();
        expect(comp.singleImageFunction()({ unified: '1F519' } as EmojiData)).toBe('public/emoji/1f519.png');
    });

    it('should emit an event on emoji select', () => {
        const emitSpy = jest.spyOn(comp.emojiSelect, 'emit');
        comp.onEmojiSelect({ test: 123 });
        expect(emitSpy).toHaveBeenCalledOnce();
        expect(emitSpy).toHaveBeenCalledWith({ test: 123 });
    });
});
