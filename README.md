# 🛡️ ContentGuard

אפליקציית אנדרואיד לחסימת תוכן לא רצוי ברמת המכשיר, עם מנגנון הגנה מפני הסרה.

## ✨ תכונות

- **VPN מקומי** – חסימת אתרים ברמת הרשת (עובד בכל דפדפן ואפליקציה)
- **DNS פרטי** – שכבת סינון נוספת דרך CleanBrowsing
- **הגנת מנהל מכשיר** – מניעת הסרה קלה של האפליקציה
- **טיימר עיכוב** – ביטול חסימה דורש המתנה של 48 שעות
- **סיסמת הגנה** – אפשרות לתת לחבר/משפחה את הסיסמה

## 🏗️ מבנה הפרויקט

```
ContentGuard/
├── app/src/main/
│   ├── java/com/contentguard/
│   │   ├── service/
│   │   │   └── BlockerVpnService.kt      ← שירות ה-VPN המרכזי
│   │   ├── receiver/
│   │   │   └── AdminReceiver.kt          ← מנהל המכשיר (הגנת הסרה)
│   │   ├── ui/
│   │   │   └── MainActivity.kt           ← מסך ראשי
│   │   └── utils/
│   │       ├── BlocklistManager.kt       ← ניהול רשימת האתרים החסומים
│   │       └── PrefsManager.kt           ← שמירת הגדרות
│   └── res/
│       ├── layout/activity_main.xml
│       ├── xml/device_admin_rules.xml
│       └── values/strings.xml
├── docs/
│   └── SETUP.md                          ← מדריך התקנה מפורט
└── README.md
```

## 🚀 התחלה מהירה

### דרישות מוקדמות
- Android Studio Hedgehog (2023.1.1) ומעלה
- JDK 17
- מכשיר/אמולטור עם Android 9 (API 28) ומעלה

### שלבי התקנה
```bash
git clone https://github.com/YOUR_USERNAME/ContentGuard.git
cd ContentGuard
# פתח ב-Android Studio ולחץ Run
```

## 🔧 איך זה עובד

```
גלישה רגילה
     ↓
[VPN מקומי על המכשיר]
     ↓
האתר ברשימה השחורה? ──YES──→ ❌ חסום
     ↓ NO
[DNS: CleanBrowsing]
     ↓
✅ גישה מורשית
```

## 🔒 הגנת הסרה

1. האפליקציה רשומה כ-Device Administrator
2. הסרה דורשת ביטול הרשאות ניהול תחילה
3. ביטול מפעיל טיימר של 48 שעות
4. ניתן לנעול עם סיסמה שנבחרת על ידי אדם אחר

## 📋 רישיון

MIT License
