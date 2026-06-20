# UI Plan — Expense Tracker (React + React Native)

Mobile-first web and mobile application. This document is the complete design and development plan — use it as the spec when building screens. Refer to `docs/backend-architecture.md` for all API contracts.

---

## Table of Contents

1. [Tech Stack & Monorepo Strategy](#1-tech-stack--monorepo-strategy)
2. [Design System](#2-design-system)
3. [Navigation Structure](#3-navigation-structure)
4. [Auth Flow](#4-auth-flow)
5. [Onboarding Flow](#5-onboarding-flow)
6. [Tab 1: Dashboard (Home)](#6-tab-1-dashboard-home)
7. [Tab 2: Transactions (Ledger)](#7-tab-2-transactions-ledger)
8. [Tab 3: Add Transaction (FAB)](#8-tab-3-add-transaction-fab)
9. [Tab 4: Budget](#9-tab-4-budget)
10. [Tab 5: Reports](#10-tab-5-reports)
11. [Settings & Profile Drawer](#11-settings--profile-drawer)
12. [Accounts Management](#12-accounts-management)
13. [Categories & Tags](#13-categories--tags)
14. [Recurring Transactions](#14-recurring-transactions)
15. [State Management & API Layer](#15-state-management--api-layer)
16. [Error & Empty States](#16-error--empty-states)
17. [Development Phases](#17-development-phases)

---

## 1. Tech Stack & Monorepo Strategy

### Repository structure

```
expense-tracker-ui/
├── apps/
│   ├── web/          — React 19 + Vite + TailwindCSS
│   └── mobile/       — React Native (Expo SDK 52+)
├── packages/
│   ├── ui/           — Shared component library (React Native Web compatible)
│   ├── api/          — Typed API client (axios + react-query)
│   ├── store/        — Zustand global state (auth token, user profile)
│   └── utils/        — Currency formatting, date helpers, validation
└── package.json      — Turborepo or pnpm workspaces
```

### Key libraries

| Concern | Web | Mobile | Shared |
|---------|-----|--------|--------|
| Framework | React 19 + Vite | Expo SDK 52 | — |
| Styling | TailwindCSS | NativeWind | — |
| Components | shadcn/ui (Radix) | React Native Paper / custom | `packages/ui` |
| Navigation | React Router v7 | Expo Router (file-based) | — |
| Data fetching | TanStack Query v5 | TanStack Query v5 | `packages/api` |
| Global state | Zustand | Zustand | `packages/store` |
| Charts | Recharts | Victory Native | — |
| Forms | React Hook Form + Zod | React Hook Form + Zod | — |
| Date | date-fns | date-fns | `packages/utils` |
| Token storage | localStorage | expo-secure-store | `packages/store` |

### Currency formatting

All amounts are INR by default. Use `Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })` globally via a shared `formatCurrency(amount)` utility. Never do arithmetic on displayed strings — always work with `number` or `string` passed as `totalAmount` to the API (2 decimal places, e.g., `"1500.00"`).

---

## 2. Design System

### Color palette

```
Primary         #6366F1   (Indigo-500)  — brand, buttons, active tabs
Primary Dark    #4F46E5   (Indigo-600)  — pressed states
Background      #0F0F13   (near black)  — main app bg (dark mode first)
Surface         #1C1C22   (dark card)   — cards, bottom sheets
Surface Raised  #26262E   (elevated)    — modals, drawers
Border          #2E2E38   — subtle dividers

Income          #22C55E   (Green-500)   — all income amounts
Expense         #EF4444   (Red-500)     — all expense amounts
Transfer        #94A3B8   (Slate-400)   — transfer amounts

Warning         #F59E0B   (Amber-500)   — budget 80%+ alert
Alert           #EF4444   (Red-500)     — budget 100%+ alert / errors
Milestone       #22C55E   (Green-500)   — investment 50%+ celebration
OK              #6366F1   (Indigo-500)  — normal budget state

Text Primary    #F8F8FF
Text Secondary  #9CA3AF   (Gray-400)
Text Muted      #6B7280   (Gray-500)
```

Light mode: invert backgrounds to `#F9FAFB` (surface) / `#FFFFFF` (raised), keep accent colors identical.

### Typography

| Style | Size | Weight | Use |
|-------|------|--------|-----|
| Hero Amount | 36px | 700 | Dashboard balance, add-screen amount input |
| Title | 22px | 700 | Screen titles |
| Subtitle | 17px | 600 | Card headers, section labels |
| Body | 15px | 400 | Transaction rows, descriptions |
| Caption | 13px | 400 | Dates, secondary info, tags |
| Micro | 11px | 500 | Badge labels, alert chips |

Font: **Inter** (web) / **SF Pro** system font (iOS) / **Roboto** (Android).

### Spacing system

4px base unit. Common values: 4, 8, 12, 16, 20, 24, 32, 48.

### Corner radii

- Cards: 16px
- Buttons: 12px
- Input fields: 10px
- Chips/badges: 9999px (pill)
- Bottom sheets: 24px (top corners only)

### Elevation / shadow

Cards use a subtle upward shadow (`0 4px 16px rgba(0,0,0,0.3)` on dark bg). Bottom sheets use a heavy overlay backdrop (`rgba(0,0,0,0.6)`).

### Component library (shared primitives)

All built in `packages/ui`. Every component must render correctly on both React DOM and React Native.

- `Button` — variants: `primary`, `ghost`, `destructive`, `outline`
- `Card` — surface container with optional press handler
- `Badge` — pill chip with color variants (for `budgetType`, `alertLevel`, `transactionType`)
- `AmountDisplay` — renders amount with correct color (red=expense, green=income, gray=transfer) and currency format
- `ProgressBar` — fills 0–100%, color driven by `alertLevel`
- `BottomSheet` — modal drawer (bottom on mobile, centered modal on web)
- `Avatar` / `AccountIcon` — icon per `accountType` (bank = building, CC = card, cash = wallet, loan = document)
- `CategoryPill` — round icon + label for category display
- `MonthPicker` — left/right chevron with "June 2026" label, tappable for calendar picker
- `Skeleton` — loading placeholder matching real component shape
- `EmptyState` — illustration + heading + optional CTA button
- `ErrorBanner` — dismissable inline error

---

## 3. Navigation Structure

### Mobile (Expo Router)

```
(auth)/
  login.tsx
  register.tsx

(onboarding)/
  welcome.tsx
  add-accounts.tsx
  setup-budgets.tsx

(app)/                     ← protected, requires auth
  _layout.tsx              ← Bottom Tab Bar (5 tabs)
  index.tsx                ← Tab 1: Dashboard
  transactions/
    index.tsx              ← Tab 2: Transaction list
    [id].tsx               ← Transaction detail / delete
  add.tsx                  ← Tab 3: Add (opens as modal)
  budget/
    index.tsx              ← Tab 4: Budget overview
    goals/new.tsx          ← Create goal
    goals/[id].tsx         ← Goal detail + category mapping
  reports/
    index.tsx              ← Tab 5: Reports
  settings/
    index.tsx              ← Settings root
    profile.tsx
    accounts/
      index.tsx
      new.tsx
      [id].tsx
    categories/index.tsx
    tags/index.tsx
    recurring/
      index.tsx
      new.tsx
```

### Web (React Router v7)

Same routes, minus the `(auth)` grouping syntax. Bottom Tab Bar becomes a **fixed bottom nav on mobile viewport** and a **left sidebar nav on ≥ 768px**.

### Bottom Tab Bar (mobile)

```
[Dashboard]  [Transactions]  [  +  ]  [Budget]  [Reports]
   🏠              📋         (FAB)      🎯         📊
```

- The `+` (Add) button is a floating action button raised above the tab bar, in the brand primary color.
- Active tab: primary color icon + label; inactive: muted gray.
- Tab bar: `Surface` background, top border, safe area inset respected.

---

## 4. Auth Flow

### Screens

#### Splash Screen
- Full-screen brand gradient (indigo to purple)
- App logo + name "ExpenseTracker"
- Auto-navigates to Login after 1.5s if no token, or to Dashboard if valid token found in storage

#### Login Screen

**Layout (mobile-first):**
```
┌─────────────────────────────┐
│         [Logo + App Name]   │
│                             │
│  Email ____________________│
│  Password __________________│
│                             │
│  [         Login          ] │  ← primary button
│                             │
│  Don't have an account?     │
│  [        Register        ] │  ← ghost button
└─────────────────────────────┘
```

- Email keyboard type, auto-capitalize off
- Password field with show/hide toggle
- On submit: `POST /api/v1/auth/login`
- On success: store `accessToken` + `userId` + `householdId` in secure storage → navigate to Dashboard
- Error states: inline below field ("Invalid email or password" — generic, matching API behavior)

#### Register Screen

```
┌─────────────────────────────┐
│         Create Account      │
│                             │
│  Your Name _________________│
│  Email _____________________│
│  Password __________________│
│  Confirm Password __________│
│                             │
│  [        Register        ] │
│                             │
│  Already have an account?   │
│  [         Login          ] │
└─────────────────────────────┘
```

- Client-side validation: name required, valid email, password min 8 chars, passwords match
- On submit: `POST /api/v1/users/register`
- On success: store token → navigate to **Onboarding** (first time) or Dashboard

---

## 5. Onboarding Flow

Shown only once, after first registration. Skip-able via "Set up later" on every step. Progress indicator at top (3 dots).

### Step 1: Welcome

```
┌─────────────────────────────┐
│   ● ○ ○                     │  (step indicator)
│                             │
│   [Illustration: wallet]    │
│                             │
│   Welcome, Alice!           │
│   Let's set up your         │
│   financial accounts first. │
│                             │
│   [     Get Started      ]  │
│   [     Skip for now     ]  │
└─────────────────────────────┘
```

### Step 2: Add Your First Account

Inline version of the Add Account form (see §12). A user must have at least one account to log transactions.

- Pre-selects "BANK" type
- Opening balance input prominent
- "Add Another Account" link below the save button

### Step 3: Set Up Budget Goals

Simplified version of the budget goal creation (see §9). User can add 1–3 goals here or skip entirely.

- Suggested presets: "Needs", "Wants", "Investments" as tappable chips that pre-fill name + type
- "Skip — I'll do this later" at bottom

On completion → Dashboard.

---

## 6. Tab 1: Dashboard (Home)

The primary at-a-glance view. Refreshed on every focus event (react-query `refetchOnWindowFocus`).

### Layout

```
┌─────────────────────────────────────┐
│  Good morning, Alice      [⚙️]  [👤] │  ← header
│  June 2026                          │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │  Net Balance                │    │  ← Hero Card
│  │  ₹ 1,24,500.00              │    │
│  │  Across 3 accounts          │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌──────────┐  ┌──────────────┐     │
│  │ 💳 CC    │  │ 📈 This Month│     │  ← 2-col widgets
│  │ Bracket  │  │ Income/Spend │     │
│  │ ₹15,000  │  │ +₹90k/-₹42k │     │
│  └──────────┘  └──────────────┘     │
│                                     │
│  Budget Goals ─────────────────     │
│  ┌─────────────────────────────┐    │
│  │ Wants    ████░░░  ₹7k/₹10k │    │  ← Budget cards
│  │          72%  ⚠ WARNING     │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ Needs    ██████░  ₹9k/₹12k │    │
│  │          75%                │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ Invest   ███░░░░  ₹6k/₹20k │    │
│  │          30%  🎯 Keep going │    │
│  └─────────────────────────────┘    │
│  [View all budgets →]               │
│                                     │
│  Recent Transactions ───────────    │
│  ┌─────────────────────────────┐    │
│  │ 🍕 Food  -₹450   Jun 17    │    │
│  │ 💼 Salary +₹90k  Jun 1     │    │
│  │ 📦 Amazon -₹2,300 Jun 16   │    │
│  └─────────────────────────────┘    │
│  [View all →]                       │
└─────────────────────────────────────┘
```

### Components & behavior

**Hero Balance Card**
- Sums `balance` across all active, non-credit-card accounts.
- Tapping opens the Accounts list (Settings → Accounts).

**Credit Card Bracket Widget**
- Source: `GET /api/v1/reports/credit-card-bracket`
- Shows `totalCreditCardLiability` as red amount.
- Subtitle: "You owe the bank" + `accountCount` card count.
- Tapping opens a bottom sheet listing each credit card account with individual balances.
- "Pay Bill" button inside the sheet opens the Add Transaction → TRANSFER flow, pre-populated with the CC account as destination.

**This Month Widget**
- Two rows: total income this month (green), total expense this month (red).
- Derived by summing the transaction list for the current month (cached by react-query).

**Budget Goal Cards**
- Source: `GET /api/v1/budgets/periods/current`
- One card per budget period, sorted by `alertLevel` (ALERT first, then WARNING, then OK).
- Progress bar: `spentAmount / startingBalance`, color driven by `alertLevel`.
- Alert badge:
  - `OK` → no badge (just indigo bar)
  - `WARNING` → amber chip "⚠ WARNING"
  - `ALERT` → red chip "🚨 OVER BUDGET"
  - `MILESTONE` → green chip "🎯 Halfway there!"
- Tapping a card navigates to Budget → Goal Detail.

**Recent Transactions**
- Last 5 from `GET /api/v1/reports/ledger?page=0&size=5`
- Each row: category icon (derived from categoryId, looked up against cached category list), description (note or category name), amount (color coded), date (relative: "Today", "Yesterday", "Jun 16").
- "View all" → Transactions tab.

### Quick actions (floating, above tab bar or inline below header)

Three pill buttons:
- `+ Expense` → opens Add modal pre-set to EXPENSE
- `+ Income` → opens Add modal pre-set to INCOME
- `→ Transfer` → opens Add modal pre-set to TRANSFER

---

## 7. Tab 2: Transactions (Ledger)

Full paginated chronological ledger with month filtering.

### Layout

```
┌─────────────────────────────────────┐
│  Transactions          [🔍] [Filter]│
│  ← June 2026 →                      │  ← MonthPicker
├─────────────────────────────────────┤
│  Jun 17                             │  ← date group header
│  ┌─────────────────────────────┐    │
│  │ [🍕] Food & Dining          │    │
│  │      Lunch at office        │    │
│  │      HDFC Savings  10:00 AM │    │
│  │                   -₹450.00  │    │
│  └─────────────────────────────┘    │
│                                     │
│  Jun 16                             │
│  ┌─────────────────────────────┐    │
│  │ [📦] Shopping               │    │
│  │      Amazon order           │    │
│  │      HDFC Credit  9:15 PM   │    │
│  │                  -₹2,300.00 │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ [↔️] Transfer               │    │
│  │      HDFC → ICICI CC        │    │
│  │                  ₹15,000.00 │    │
│  └─────────────────────────────┘    │
│                                     │
│  Jun 1                              │
│  ┌─────────────────────────────┐    │
│  │ [💼] Income                 │    │
│  │      Salary                 │    │
│  │      HDFC Savings           │    │
│  │                  +₹90,000   │    │
│  └─────────────────────────────┘    │
│                                     │
│  [Load more...]                     │  ← infinite scroll / load more
└─────────────────────────────────────┘
```

### Behavior

**Month picker:** Tapping left/right arrows changes the month filter. Tapping the label opens a scrollable month-year picker bottom sheet. Default: current month.

**Transaction row:**
- Left: category icon (emoji or icon based on category type) in a colored circle.
- Middle: category name (or `customAlias` if set) on top, `note` or account name below, time on the right of middle.
- Right: amount, color-coded.
  - EXPENSE → red `-₹X`
  - INCOME → green `+₹X`
  - TRANSFER → gray `₹X` with `↔` prefix
- Split transactions: show the parent total, with a subtle "split" badge. Tapping shows detail.

**Tapping a row:** Opens the Transaction Detail bottom sheet.

**Transaction Detail Sheet:**
```
┌─────────────────────────────────────┐
│  [Category Icon]   Expense          │
│  ₹1,500.00                          │
│  Jun 17, 2026 · 10:00 AM            │
│                                     │
│  Account: HDFC Savings              │
│  Note: Lunch at office              │
│                                     │
│  Line Items                         │
│  ─────────────────────────────────  │
│  Food & Dining · Restaurants  ₹1000 │
│  Beverages                     ₹500 │
│                                     │
│  Tags: Online, Swiggy               │
│                                     │
│  Budget Goal: Monthly Wants         │
│                                     │
│         [Delete Transaction]        │  ← destructive, confirmation required
└─────────────────────────────────────┘
```

**Delete:** Confirmation dialog ("This cannot be undone. The budget tracker will not automatically revert.") → `DELETE /api/v1/transactions/{id}` → refresh transaction list and dashboard.

**Infinite scroll / Load more:** Page through `GET /api/v1/reports/ledger?month=&year=&page=&size=20`. Show a skeleton row while fetching the next page. Show "You've reached the beginning" when `currentPage == totalPages - 1`.

---

## 8. Tab 3: Add Transaction (FAB)

The primary action of the app. Opens as a **bottom sheet modal** (95% height on mobile, centered modal on wide screens). Pre-set to EXPENSE by default.

### Transaction type selector

Three horizontally-scrolling pill tabs at the top of the sheet:
```
[ Expense ]  [ Income ]  [ Transfer ]
```
Switching type morphs the form below without closing the sheet.

---

### EXPENSE Form

```
┌─────────────────────────────────────┐
│  [×]           Expense         [✓] │
│  ─────────────────────────────────  │
│  Type: [Expense] [Income] [Transfer]│
│                                     │
│         ₹  [  0.00  ]               │  ← Hero amount input, large
│                                     │
│  Date   [Today, Jun 17 ▾]           │
│  From   [Select Account ▾]          │
│  Note   [Add a note... ]            │
│                                     │
│  ─── Line Items ────────────────    │
│  ┌─────────────────────────────┐    │
│  │ Category  [Select... ▾]     │    │  ← Line item 1
│  │ Amount    [₹ 0.00       ]   │    │
│  │ Budget    [Auto / Manual ▾] │    │  ← optional override
│  │ Tags      [+ Add tags   ]   │    │
│  └─────────────────────────────┘    │
│  [+ Add another split]              │
│                                     │
│  Split balance: ₹0 of ₹0 allocated │  ← live validation bar
│  [remaining: ₹0.00]                 │
│                                     │
│        [  Save Expense  ]           │
└─────────────────────────────────────┘
```

**Amount input:**
- Large centered number. User taps and types (number keyboard).
- Updates the "remaining" indicator in real time.

**Date picker:**
- Default: today.
- Tapping opens a native date picker (DateTimePicker on RN, HTML date input on web).
- Recent dates shown as quick chips: "Today", "Yesterday", "2 days ago".

**Account picker:**
- Bottom sheet list of user's active accounts, each with icon, name, balance.
- Shows account type badge.

**Line items:**
- At least one required.
- Each line item has:
  - **Category selector:** Opens a 2-level category browser (roots as sections, sub-categories as selectable rows). Shows `customAlias` where set.
  - **Amount field:** Defaults to the full `totalAmount` when only one line item exists. Auto-adjusts if multiple.
  - **Budget goal override** (optional): Dropdown showing active goals. Default label "Auto (from category mapping)". Select a specific goal to override the default routing.
  - **Tags multi-select:** Chip list of available tags. Tap to select/deselect. "+" to create a new personal tag inline.

**Split balance tracker:**
- Live bar below line items.
- Green when `sum(lineItems) == totalAmount`.
- Red when `sum(lineItems) != totalAmount` (blocks save button).
- Shows allocated vs total: "₹1,000 of ₹1,500 allocated — ₹500 remaining".
- The "Save" button is disabled and grayed out until the balance is zero.

**Validation (client-side, before API call):**
- Amount > 0
- At least one line item
- Each line item has categoryId and amount > 0
- Sum of line item amounts == totalAmount (enforced visually and programmatically)
- Source account selected

---

### INCOME Form

Simpler — no split line items needed for typical income.

```
┌─────────────────────────────────────┐
│  [×]           Income          [✓] │
│  ─────────────────────────────────  │
│         ₹  [  0.00  ]               │
│                                     │
│  Date   [Today ▾]                   │
│  To     [Select Account ▾]          │  ← "To" not "From" for income
│  Category [Select Category ▾]       │
│  Note   [Add a note... ]            │
│  Tags   [+ Add tags]                │
│                                     │
│        [  Save Income  ]            │
└─────────────────────────────────────┘
```

Maps to a single line item matching the total amount. Uses `sourceAccountId` = the destination account (income arrives in an account).

---

### TRANSFER Form

```
┌─────────────────────────────────────┐
│  [×]          Transfer         [✓] │
│  ─────────────────────────────────  │
│         ₹  [  0.00  ]               │
│                                     │
│  Date   [Today ▾]                   │
│  From   [Select Account ▾]          │
│  To     [Select Account ▾]          │
│  Note   [Add a note... ]            │
│                                     │
│  ℹ️ Transfers don't affect budgets  │
│                                     │
│  ─── Credit Card Payment? ──────    │  ← shown only if "To" = CC account
│  Your CC bracket is ₹15,000.        │
│  This transfer: ₹15,000.            │
│  ✅ Bracket will be cleared.        │
│  (or)                               │
│  ⚠️ Transfer (₹16,000) > bracket    │
│  (₹15,000). Log ₹1,000 as expense? │
│  [Log extra as expense]             │
│                                     │
│        [  Save Transfer  ]          │
└─────────────────────────────────────┘
```

**CC payment detection:**
- When "To" account is a `CREDIT_CARD` type, show the CC bracket info section.
- Compare entered amount vs `totalCreditCardLiability`.
- If amount > bracket: show warning and "Log extra as expense" button — this opens a secondary flow to create a new EXPENSE transaction for the difference amount.
- Single line item created with `categoryId = null` (enforced by the backend for transfers).

---

## 9. Tab 4: Budget

Manage budget goals, track monthly periods, and configure category-to-goal mappings.

### Budget Overview Screen

```
┌─────────────────────────────────────┐
│  Budget              [+ New Goal]   │
│  ← June 2026 →                      │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │ 🚨 Wants             ALERT  │    │  ← ALERT card (red tint)
│  │ ₹10,500 spent of ₹10,000   │    │
│  │ ████████████ 105%           │    │
│  │ -₹500 over budget           │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ ⚠️ Needs             WARNING │    │  ← WARNING card (amber tint)
│  │ ₹9,200 spent of ₹12,000    │    │
│  │ ████████░░░  77%            │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 🎯 Investments     MILESTONE│    │  ← MILESTONE card (green tint)
│  │ ₹11,000 of ₹20,000         │    │
│  │ ██████░░░░░  55%            │    │
│  │ Great progress!             │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ ✅ Custom Goal        OK    │    │
│  │ ₹2,000 of ₹8,000           │    │
│  │ ██░░░░░░░░  25%             │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

**API:** `GET /api/v1/budgets/periods/current` for current month periods.
**Sort order:** ALERT → WARNING → MILESTONE → OK.

**Month picker:** Historical months show read-only snapshots (API doesn't support historical periods yet — show "Historical data coming soon" for past months).

**Tapping a goal card:** Opens Goal Detail screen.

---

### Goal Detail Screen

```
┌─────────────────────────────────────┐
│  ← Monthly Wants           [Edit]   │
│                                     │
│  WANTS  •  Active                   │
│                                     │
│  Target   ₹10,000/month             │
│  Starting ₹10,200  (rollover +₹200) │
│  Spent    ₹7,500                    │
│  Left     ₹2,700                    │
│                                     │
│  ████████░░░  73%  ⚠ WARNING       │
│                                     │
│  ─── Linked Categories ──────────   │
│  ┌─────────────────────────────┐    │
│  │ 🍕 Food & Dining [DEFAULT]  │    │
│  │ 🛍️ Shopping                 │    │
│  │ 🎬 Entertainment            │    │
│  │ [+ Link Category]           │    │
│  └─────────────────────────────┘    │
│                                     │
│  [  Delete Goal  ] ← destructive    │
└─────────────────────────────────────┘
```

**API calls:**
- `GET /api/v1/budgets/goals/{id}/categories` for linked categories
- `POST /api/v1/budgets/goals/{id}/categories` to add mapping
- `DELETE /api/v1/budgets/goals/{id}/categories/{categoryId}` to remove

**Link Category:**
Opens a category browser (same 2-level tree as Add Transaction). Selecting a category calls `POST /api/v1/budgets/goals/{id}/categories`.

**DEFAULT badge:** One category per goal can be marked as the default. Swipe left on a category row → "Set as Default" / "Remove" actions.

**Rollover note:** If `startingBalance != targetAmount`, show explanation: "(rollover +₹200 from last month)" or "(deficit -₹500 from last month)".

---

### Create Budget Goal Screen

```
┌─────────────────────────────────────┐
│  ←   New Budget Goal                │
│                                     │
│  Goal Type                          │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌────┐ │
│  │Needs │ │Wants │ │Invest│ │Cust│ │  ← type selector chips
│  └──────┘ └──────┘ └──────┘ └────┘ │
│                                     │
│  Goal Name                          │
│  [Monthly Wants              ]      │
│                                     │
│  Monthly Target                     │
│  ₹ [10,000                  ]       │
│                                     │
│  About this type:                   │
│  Wants: Daily lifestyle spends.     │
│  Warning at 80%, Alert at 100%.     │
│                                     │
│        [Create Goal]                │
└─────────────────────────────────────┘
```

**API:** `POST /api/v1/budgets/goals`

**Type descriptions:**
- NEEDS: Essential living expenses. Warning at 80%, Alert at 100%.
- WANTS: Lifestyle, entertainment. Warning at 80%, Alert at 100%.
- INVESTMENTS: Savings and growth goals. Milestone at 50%, Alert at 100%.
- CUSTOM: Fully customizable. Alert at 100% only.

After creation, prompt: "Link categories to this goal?" → opens category browser.

---

## 10. Tab 5: Reports

Visual spending analytics for the selected month.

### Reports Screen

```
┌─────────────────────────────────────┐
│  Reports                            │
│  ← June 2026 →                      │
├─────────────────────────────────────┤
│  ─── Spending Breakdown ─────────   │
│                                     │
│         [Donut Chart]               │  ← center: total spent
│     🍕 Food 28%                     │
│     🛍 Shopping 22%                 │
│     🏠 Housing 35%                  │
│     🎬 Entertainment 15%            │
│                                     │
│  ─── By Category ────────────────   │
│  ┌─────────────────────────────┐    │
│  │ 🏠 Housing           ₹14k  │    │
│  │     ███████████████  35%   │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🍕 Food & Dining    ₹11.2k │    │
│  │     ███████████     28%    │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🛍 Shopping          ₹8.8k │    │
│  │     █████████        22%   │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🎬 Entertainment      ₹6k  │    │
│  │     ██████            15%  │    │
│  └─────────────────────────────┘    │
│                                     │
│  ─── Summary ────────────────────   │
│  Income this month    +₹90,000      │
│  Expenses this month  -₹40,000      │
│  Net                   ₹50,000 ✅   │
└─────────────────────────────────────┘
```

**APIs:**
- `GET /api/v1/reports/breakdown?month=6&year=2026` → category breakdown (list of `{ categoryId, total }`)
- `GET /api/v1/categories` → cached category list to resolve names and parent/child hierarchy
- `GET /api/v1/reports/ledger?month=6&year=2026&size=1000` → for income/expense summary totals (or compute from breakdown + filter by type)

**Donut chart:**
- Center: total EXPENSE amount for the month.
- Segments: one per root category, color randomized from a fixed palette per category.
- Legend below (category name + percentage).
- Only shows root-level categories (group sub-categories into parent for the chart).

**By Category list:**
- Sorted by total descending.
- Tap a row → drill down to see sub-category breakdown in a new screen.
- Horizontal bar behind each row proportional to percentage of total spend.

**Summary strip:**
- Total income (green) + total expenses (red) + net (green if positive, red if negative).

---

## 11. Settings & Profile Drawer

Accessible via the gear icon (⚙️) in the Dashboard header, or as a 6th tab on wide screens.

### Settings Root

```
┌─────────────────────────────────────┐
│  ←  Settings                        │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │ 👤  Alice                   │    │
│  │     alice@example.com       │    │
│  └─────────────────────────────┘    │
│                                     │
│  FINANCE                            │
│  [💳] Accounts              →       │
│  [📂] Categories            →       │
│  [🏷️]  Tags                  →       │
│  [🔁] Recurring Transactions →       │
│                                     │
│  APP                                │
│  [🌙] Dark Mode             [toggle]│
│  [💱] Currency (INR)        →       │
│                                     │
│  [  Log Out  ]                      │
└─────────────────────────────────────┘
```

---

## 12. Accounts Management

### Accounts List (Settings → Accounts)

```
┌─────────────────────────────────────┐
│  ←  Accounts              [+ Add]   │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │ 🏦 HDFC Savings    BANK     │    │
│  │    ₹1,24,500.00             │    │
│  │    INR                      │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 💳 ICICI Credit  CC         │    │
│  │    ₹-15,000.00  (liability) │    │
│  │    Cycle: 16th – 15th       │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 💵 Cash Wallet   CASH       │    │
│  │    ₹2,000.00                │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

**Account type icons:** 🏦 BANK, 💳 CREDIT_CARD, 💵 CASH, 📋 LOAN

Tapping an account opens the Edit Account sheet. Swipe left → Archive (sets `isActive = false`).

### Add / Edit Account Sheet

```
┌─────────────────────────────────────┐
│  Add Account                        │
│                                     │
│  Account Type                       │
│  [ Bank ] [Credit Card] [Cash][Loan]│
│                                     │
│  Account Name                       │
│  [HDFC Savings              ]       │
│                                     │
│  Opening Balance                    │
│  ₹ [50,000.00               ]       │
│                                     │
│  ── Credit Card Only ──────────     │  ← shown only for CC type
│  Billing Start Day  [16]            │
│  Billing End Day    [15]            │
│                                     │
│       [  Save Account  ]            │
└─────────────────────────────────────┘
```

**API:** `POST /api/v1/accounts` (create) or `PATCH /api/v1/accounts/{id}` (edit — name and isActive only).

**Note for edit mode:** Opening balance and account type cannot be changed after creation (no backend support in MVP1). Show a disabled field with "(set at creation)" label.

---

## 13. Categories & Tags

### Categories Screen (Settings → Categories)

```
┌─────────────────────────────────────┐
│  ←  Categories                      │
│  Rename categories for yourself.    │
├─────────────────────────────────────┤
│  ─── Expense ────────────────────   │
│  ┌─────────────────────────────┐    │
│  │ 🍕 Food & Dining           →│    │  ← root category, expandable
│  │   └ Restaurants             │    │
│  │   └ Groceries               │    │
│  │   └ Coffee & Tea            │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🛍 Shopping                →│    │
│  │   └ Clothing                │    │
│  │   └ Electronics             │    │
│  └─────────────────────────────┘    │
│  ...                                │
│  ─── Income ─────────────────────   │
│  ┌─────────────────────────────┐    │
│  │ 💼 Salary & Income          │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

**API:** `GET /api/v1/categories` (auto-seeds if first time).

Tapping a category opens a rename sheet:
```
┌─────────────────────────────────────┐
│  Rename Category                    │
│  System name: Food & Dining         │
│                                     │
│  Your label [Food & Dining    ]     │
│                                     │
│  [Clear alias]   [Save]             │
└─────────────────────────────────────┘
```

**API:** `PUT /api/v1/categories/mapping/{categoryId}` with `{ customAlias: "..." }` or `null` to clear.

### Tags Screen (Settings → Tags)

Two sections: Global Tags (read-only) and My Tags (deletable on long-press? — delete not in API yet, so just view).

```
┌─────────────────────────────────────┐
│  ←  Tags                  [+ New]   │
├─────────────────────────────────────┤
│  GLOBAL                             │
│  [Online] [Offline] [EMI] [Recurring]
│  [Reimbursable] [Business] [Gift]   │
│                                     │
│  MINE                               │
│  [Swiggy] [Weekend] [+ New Tag]     │
└─────────────────────────────────────┘
```

"+ New" → inline bottom sheet:
```
Tag name [ Weekend Spend ]
[Create]
```

**API:** `POST /api/v1/tags` with `{ name: "..." }`.

---

## 14. Recurring Transactions

Accessible from Settings → Recurring Transactions.

### Recurring List

```
┌─────────────────────────────────────┐
│  ←  Recurring               [+ Add] │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │ 📺 Netflix         MONTHLY  │    │
│  │    ₹999 · Next: Jul 1       │    │
│  │    HDFC Savings             │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🏠 Rent            MONTHLY  │    │
│  │    ₹25,000 · Next: Jul 1    │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 💪 Gym              YEARLY  │    │
│  │    ₹6,000 · Next: Jan 1     │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

**API:** `GET /api/v1/recurring-transactions`

Swipe left on a row → Cancel (soft-delete). Confirmation: "This will stop future auto-posts. Past transactions are unaffected."

**API:** `DELETE /api/v1/recurring-transactions/{id}`

### Create Recurring Transaction

Reuses the same Add Transaction form (EXPENSE or INCOME only — no recurring transfers) with two extra fields added at the bottom:

```
─── Recurrence ──────────────────────
Frequency  [Monthly ▾]
First run  [Jul 1, 2026 ▾]

[  Save Recurring Transaction  ]
```

On submit: builds `headerTemplate` and `lineItemsTemplate` from the form and calls `POST /api/v1/recurring-transactions`.

**Template building (client-side):**
```js
headerTemplate = {
  sourceAccountId: selectedAccountId,
  transactionType: "EXPENSE",
  totalAmount: amount.toFixed(2),
  note: noteValue
}
lineItemsTemplate = lineItems.map(li => ({
  categoryId: li.categoryId,
  amount: li.amount.toFixed(2),
  tagIds: li.tagIds,
  budgetGoalId: li.budgetGoalId ?? null
}))
```

---

## 15. State Management & API Layer

### Auth state (Zustand `packages/store`)

```ts
interface AuthStore {
  accessToken: string | null
  userId: string | null
  householdId: string | null
  setAuth: (token: string, userId: string, householdId: string) => void
  clearAuth: () => void
}
```

Token is persisted to `localStorage` (web) or `expo-secure-store` (mobile) on `setAuth`.

### API client (`packages/api`)

Single `axios` instance with:
- `baseURL: process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080'`
- Request interceptor: attach `Authorization: Bearer <token>` from store
- Response interceptor: on 401 → clear auth store → redirect to login

### TanStack Query key conventions

```ts
['categories']                               // GET /api/v1/categories
['tags']                                     // GET /api/v1/tags
['accounts']                                 // GET /api/v1/accounts
['transactions', { month, year, page }]      // GET /api/v1/reports/ledger
['transaction', id]                          // GET /api/v1/transactions/{id}
['budget-periods']                           // GET /api/v1/budgets/periods/current
['budget-goals']                             // GET /api/v1/budgets/goals
['goal-categories', goalId]                  // GET /api/v1/budgets/goals/{id}/categories
['breakdown', { month, year }]               // GET /api/v1/reports/breakdown
['cc-bracket']                               // GET /api/v1/reports/credit-card-bracket
['recurring']                                // GET /api/v1/recurring-transactions
['me']                                       // GET /api/v1/users/me
```

### Cache invalidation after mutations

| Action | Invalidate |
|--------|-----------|
| Create transaction | `['transactions']`, `['budget-periods']`, `['cc-bracket']` |
| Delete transaction | `['transactions']`, `['budget-periods']`, `['cc-bracket']` |
| Create account | `['accounts']`, `['cc-bracket']` |
| Create budget goal | `['budget-goals']`, `['budget-periods']` |
| Add category mapping | `['goal-categories', goalId]` |
| Update category alias | `['categories']` |
| Create tag | `['tags']` |

---

## 16. Error & Empty States

### Empty states

| Screen | Condition | Message | CTA |
|--------|-----------|---------|-----|
| Transactions | No transactions this month | "No transactions in June. Start tracking!" | "Add Expense" button |
| Budget Overview | No goals created | "No budget goals yet. Set your first goal." | "Create Goal" button |
| Recurring | No recurring set up | "No recurring transactions." | "Add Recurring" button |
| Accounts | No accounts | "Add your first account to start." | "Add Account" button |
| Reports | No data | "Nothing to report for this month." | — |

### API error handling

- **Network error / 5xx:** Toast notification "Something went wrong. Please try again." Retry button where applicable.
- **401:** Auto-redirect to Login (handled in axios interceptor globally).
- **422 Ledger Imbalance:** Inline banner inside the Add Transaction sheet: "Line items don't add up to ₹X. Please review your split."
- **400 Validation:** Map `fieldErrors` from API response to inline field errors in form (React Hook Form `setError`).
- **404:** "Not found" banner, back navigation.

### Loading states

Every list and data screen shows skeleton placeholders (matching the shape of the real content) while the initial fetch is in flight. Use `isLoading` from react-query, not `isFetching`, so background refreshes don't flash skeletons.

---

## 17. Development Phases

Build in this order — each phase is independently shippable.

### Phase 1 — Foundation (Auth + Accounts + Navigation)

**Goal:** User can register, log in, create accounts, and navigate the empty shell.

- Monorepo setup (Turborepo + pnpm)
- Design system setup (colors, typography, shared `packages/ui` primitives)
- Auth screens (Login, Register)
- Bottom tab navigation shell (5 tabs, all empty)
- Auth state + API client + token persistence
- Accounts CRUD (Add Account sheet + Account list in Settings)
- Dashboard shell with account balance widget (hardcoded structure, live data)

**Deliverable:** App boots, user registers, sees account balances.

---

### Phase 2 — Transactions (Core Loop)

**Goal:** User can add, view, and delete transactions. This is the single most important flow.

- Add Transaction sheet (EXPENSE + INCOME, single line item only)
- Category picker (2-level browser)
- Transaction list screen with month picker
- Transaction detail bottom sheet + delete
- Dashboard "Recent Transactions" widget
- Split transaction support (multiple line items + balance tracker)
- TRANSFER type

**Deliverable:** Full transaction log working. Budget is not yet wired.

---

### Phase 3 — Budget & Dashboard

**Goal:** Budget goals visible and updating as transactions are logged.

- Budget overview screen (period cards with progress bars + alert levels)
- Create Budget Goal screen
- Goal Detail screen + category mapping management
- Dashboard budget cards + alert level badges
- CC bracket widget (reporting API)

**Deliverable:** Full dashboard functional. User can see spending against goals.

---

### Phase 4 — Reports

**Goal:** Visual spending analytics.

- Reports screen: donut chart + by-category list
- Month/year picker
- Summary strip (income vs expense vs net)
- Sub-category drill-down

**Deliverable:** Full reporting screen with real data.

---

### Phase 5 — Settings, Recurring & Polish

**Goal:** Complete settings, recurring transactions, onboarding.

- Onboarding flow (3 steps)
- Categories screen (rename aliases)
- Tags screen (view + create personal tags)
- Recurring transactions list + creation + cancel
- Dark/light mode toggle
- Empty state illustrations
- Pull-to-refresh on all list screens
- Performance: lazy loading, image caching, tab prefetching

**Deliverable:** Production-ready MVP1 UI.

---

### Phase 6 — Web-specific

**Goal:** Web app at parity with mobile.

- Responsive layout (sidebar nav on ≥768px, bottom tab on mobile)
- Web date/amount inputs native behavior
- Keyboard navigation (tabs, enter-to-submit)
- Browser tab title management
- PWA manifest + service worker for offline shell

**Deliverable:** Deployable web app.
