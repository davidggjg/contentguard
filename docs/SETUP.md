# 📖 מדריך הגדרה מפורט – ContentGuard

## שלב 1: פתיחת הפרויקט ב-Android Studio

1. פתח את Android Studio
2. בחר **File → Open**
3. נווט לתיקיית `ContentGuard` ולחץ OK
4. המתן עד שה-Gradle Sync יסתיים (כמה דקות בפעם הראשונה)

---

## שלב 2: חיבור מכשיר / הפעלת אמולטור

### אמולטור (מומלץ לפיתוח):
- **Tools → Device Manager → Create Device**
- בחר Pixel 6, API 34

### מכשיר אמיתי:
1. **הגדרות → על הטלפון → מספר build** – לחץ 7 פעמים
2. **הגדרות → אפשרויות מפתח → USB Debugging** – הפעל
3. חבר כבל USB

---

## שלב 3: הרצה ראשונה

לחץ ▶️ (Run) ב-Android Studio.

האפליקציה תתקין ותפתח על המכשיר.

---

## שלב 4: הגדרת DNS פרטי (שכבה נוספת)

זה נפרד מה-VPN ומוסיף שכבת סינון:

1. **הגדרות → חיבורים → עוד הגדרות חיבור → DNS פרטי**
2. בחר **שם מארח ספק DNS פרטי**
3. הקלד: `adult-filter-dns.cleanbrowsing.org`
4. לחץ שמור

---

## שלב 5: הפעלת הגנת Admin

באפליקציה:
1. לחץ **"הפעל הגנת הסרה"**
2. אשר בדף ההרשאות שיפתח
3. ✅ עכשיו מחיקת האפליקציה דורשת כמה שלבים נוספים

---

## 🔧 TODO – השלמת הקוד (לפי רמת קושי)

### קל – מותאם למתחיל:
- [ ] הוסף עוד דומיינים לרשימה ב-`BlocklistManager.kt`
- [ ] שנה את צבעי הממשק ב-`activity_main.xml`
- [ ] הוסף שפות נוספות ב-`res/values-en/strings.xml`

### בינוני:
- [ ] מימוש DNS packet parsing ב-`BlockerVpnService.kt`
  - קרא על: DNS wire format, ByteBuffer parsing
- [ ] הוספת סיסמת PIN עם bcrypt hashing
- [ ] מסך "סטטיסטיקות" – כמה חסימות היום/השבוע

### מתקדם:
- [ ] הורדה אוטומטית של blocklist מ-StevenBlack GitHub
- [ ] Room Database לשמירת blocklist מקומית
- [ ] WorkManager לעדכון שבועי של הרשימה
- [ ] ווידג'ט홈מסך שמציג מצב הגנה

---

## 📚 קריאה נוספת

- [VpnService Android Docs](https://developer.android.com/reference/android/net/VpnService)
- [DevicePolicyManager Docs](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)
- [StevenBlack Hosts Lists](https://github.com/StevenBlack/hosts)
- [CleanBrowsing DNS](https://cleanbrowsing.org/filters)
