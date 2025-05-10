# pr_ai_reviewer.py
# Reviews pull requests, diffs, and suggests test cases or flags.

from ai_engine import run_ai_analysis

def review_diff(diff_text):
    return run_ai_analysis(f"Review this Git diff and comment on its quality and potential issues:

{diff_text}")

def suggest_test_cases_for_diff(diff_text):
    return run_ai_analysis(f"Suggest tests for the following Git diff:

{diff_text}")
