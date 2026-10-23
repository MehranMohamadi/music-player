Noor 1.1.0 — موج صدا و ایموجی‌های روی کاور

برای نصب یا به‌روزرسانی، فایل **Noor.apk** را از بخش Assets دانلود کنید. اندروید ۱۱ یا بالاتر لازم است. امضای این نسخه با نسخهٔ قبلی یکسان است.

- نوار پخش خاکستری با موج واقعی فایل صوتی؛ قسمت پخش‌شده سبز می‌شود و همچنان می‌توانید با لمس یا کشیدن، زمان پخش را تغییر دهید.
- انتخابگر ۱۰ ایموجی روی کاور و امکان وارد کردن ایموجی دلخواه از کیبورد، با انیمیشن ورود و جای تصادفی؛ هر انتخاب یک نمونهٔ جداست و محدودیتی برای تعداد یا تکرار وجود ندارد.
- ایموجی‌ها، جای قرارگیری و چرخش آن‌ها برای هر آهنگ روی خود گوشی ذخیره می‌شوند و با باز کردن دوبارهٔ برنامه یا اسکن کتابخانه باقی می‌مانند. با لمس هر ایموجی می‌توانید همان نمونه را حذف کنید.
- بخش **کتابخانه ← ایموجی‌ها** برای دسته‌بندی آهنگ‌ها بر اساس ایموجی؛ تکرار ایموجی باعث تکرار آهنگ در فهرست نمی‌شود.
- ارتقای دیتابیس، آهنگ‌ها، پسندیده‌ها، سابقهٔ پخش و مجموعه‌های قبلی را حفظ می‌کند. فایل صوتی و تصویر کاور اصلی تغییر نمی‌کنند. ذخیره‌سازی محلی است و مجوز اینترنت یا میکروفون اضافه نشده است.

**Build:** optimized Release APK with code/resource shrinking, signed with the existing development/test key. This is an evaluation build, not a production-signed store release. No signing key is included in the repository.

**Validation:** 31 unit tests pass, including database migration/reopen, repeated emoji persistence/filtering, Unicode emoji input, and waveform RMS/timeline regressions. Android lint reports zero errors. Debug and device-test APK builds pass. Physical-device UI animation, seeking, audible playback during waveform analysis and device codec behavior remain unverified; device tests compile but have not run in this cloud worker. See `docs/VALIDATION.md`.

`SHA256SUMS` contains the SHA-256 of the attached APK. Its path inside the repository is `distribution/Noor.apk`; after downloading both assets, compare with `sha256sum Noor.apk`.
