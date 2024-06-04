from django.shortcuts import render, redirect
from django.views.generic import View

from .helpers import accounts_helper

class AccountsView(View):
    def get(self, request):
        accounts = accounts_helper.get_all_accounts(request)
        return render(request, 'accounts/accounts.html', {
            "accounts": accounts
        })

class AccountDetailsView(View):
    def get(self, request, provider: str, cell: str, id: str):
        account = accounts_helper.get_by_cell_and_id(request, cell, id, provider)
        if account is None:
            return redirect('/clouds/accounts')
        return render(request, 'accounts/account_details.html', {
            "account": account
        })
