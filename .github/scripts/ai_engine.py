# ai_engine.py
# Core logic hub for all AI-driven tasks: autofix, refactor, suggest, and retrospective.

import os
import openai

openai.api_key = os.getenv("OPENAI_API_KEY")

def run_ai_analysis(prompt, temperature=0.3):
    response = openai.ChatCompletion.create(
        model="gpt-4-turbo",
        messages=[
            {"role": "system", "content": "You're a senior software engineer helping refactor code."},
            {"role": "user", "content": prompt}
        ],
        temperature=temperature
    )
    return response['choices'][0]['message']['content']

def analyze_code_for_smells(code_str):
    return run_ai_analysis(f"Analyze the following code for bugs, smells, and improvement suggestions:\n\n{code_str}")

def suggest_test_cases(code_str):
    return run_ai_analysis(f"Suggest unit tests for this code:\n\n{code_str}")

def fix_workflow_error(error_log, yaml_str):
    return run_ai_analysis(f"Fix the following GitHub Actions YAML based on this error:\n\n{error_log}\n\nYAML:\n{yaml_str}")
