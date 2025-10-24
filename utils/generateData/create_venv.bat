@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo Creating Python virtual environment...
python -m venv venv

echo Activating virtual environment...
call venv\Scripts\activate.bat

echo Upgrading pip...
python -m pip install --upgrade pip

echo Installing dependencies...
pip install -r requirements.txt

echo Virtual environment setup complete!
echo Use the following command to activate:
echo venv\Scripts\activate.bat
pause