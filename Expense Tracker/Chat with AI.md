I am planning to create an expense tracker application for my daily expense tracking purpose.

Here is the functional requirements for the same.

1) Record daily expense and income -> Credit and Debit
2) Have Categories in the following format: (Graph/tree like structure)
    a) Root Category.
    b) sub category level 1
    c) sub category level 2
    d) sub category level 3
    e) sub category level 4
    Example: Shopping -> Clothing -> Store -> Online/Offline.
    But there is a challenge, the sub categories can immediately spiral out of control. So setting them to max 4 is also not correct.
    Example: Shopping -> Clothing -> Footwear -> Store -> Online/Offline
    This is still a challenge which we need to solve.
3) Record the Date of payment
4) Record the mode of payment
    1) UPI.
    2) Credit Card.
    3) Cash.
    4) Debit Card.
    5) Debt/Loan
5) If the payment is made from credit card, 
    1) Track the billing cycle.
    2) Based on the payment of the billing cycle, Calculate the current month's expense.
    Example if the current billing cycle is from 16th of current month (June) to 15th of next month (July), I need to pay the bill before 5th of August.
    I am planning to settle the bill before Last day of (July).
    Add this expense in the respective month's expense.
6) For now make all the entries of expense/income as manual.
7) Have the Goal brackets and map them to the parent category. (one to one mapping)
    a) Investment
    b) Necessities
    c) Wants
    d) etc.
    Each Goal brackets will have a threshold for spend. Send a warning if the threshold is breached.
    For Example: 
        1) If Wants is spent more than 30k per month, send warning on the dashboard.
        2) If Investment is spent more than 1 lac per month, send a green reward on a dashboard.
8) This shall be MVP1 for this application.
9) Analyze all the requirements and gaps in the design and Let's brainstorm together


Solving Problems

Great analysis, Let's address each point one by one.

1. Solving the Category Spiral (Requirement 2)
    Use the tags on UI and adjacency list on backend.
2. The Credit Card Conundrum (Requirement 5)
    Good choice to use Transfers or wallets.
    Just add one more thing.
    Create a separete future expense bracket which will track the upcoming bills of credit card.
    Example, if I buy the shirt on June 20th using the HDFC Credit Card, it is recorded as an Expense immediately. My credit card balance goes to -₹2,000. Your June "Wants" budget is updated. Along with that, the future expense of that card is also updated, which tells me that I owe 2,000 Rs to the bank and I shall keep this in mind. Once payment is done, this bracket is cancelled.
    So, create another Category as Credit Card bill payment and adjust the value from Future expense bracket. If the Credit card bill payment exceeds the future expense bracket, Prompt the user to adjust the remaining amount as an additional expense.
3. Goal Brackets & Thresholds (Requirement 7)
    Allow the users to set their Goals and map them
4. Uncovering Hidden Gaps for MVP1
    Add all the information you feel is missing with this design


Solving more problems

1. The "Oops" Factor: CRUD & Rollbacks
    a) Let's implement Strict Transaction across the operations and Incorporate correct rollback strategies.

2. Budget Rollover Logic
This shoul be a zero-based system for now. 
If the month is ended, reset all the brackets to their defaults (As set by the user).
If additional amount is saved, Mark it as an income for the next month.
If extra amount is debited, Mark it as expense from last month -> there should be a new category for Expense from last month. Analyze from which goal bracket was this transaction made and debit the expense from that bracket the next month.
For example, If current month's budget of wants was 30k and after the month is over, I overspent on the Wants to 35k, Then in the next month auto debit the 5k under category -> Expense from last month under the bracket of wants.

3. Recurring Transactions vs. Strictly Manual
    a) Give an option to add a recurring transaction
    b) Also give an option to duplicate a transation from last month.

4. Reporting & Historical View
    a) Yes, users will have all kinds of retrospective data for the spends.

5. Data Portability (Export)
    a) Keep this as an option for MVP 2

Solving more problems

1. CRUD & Strict Transactions
    a) We will be using Spring boot and its Transactional capabilities

2. Budget Rollover Logic (The Reality Check)
    a) Categorize the extra income as Rollover balance

Where do you plan to store this data—are we building this as a local-storage app (where data lives only on the user's phone, requiring app-open triggers for recurring items) or a cloud-synced app with a backend database (allowing server-side cron jobs)?

Let's discuss this as a part of technical discussion. Once we have the functional knowledge ready, we can move to technical part.


Will this application be strictly for a single user tracking one currency (e.g., INR), or do we need to functionally account for multiple profiles (like a family sharing a budget) or multi-currency transactions (like tracking travel expenses) in MVP1?

Currently this will be for single user but we can extend it for family. 
Currency can also be extended for MVP2


Technical

1) Spring Boot with Spring cloud for backend
2) Postgres for Relational DB
3) MongoDB for non Relational - If required
4) UI will be built on React Native & React -> For Web and mobile applications
Once this is set, let's dive deep into Database Design.
5) Each table should be precise and fail proof. Let's go over each entity one by one