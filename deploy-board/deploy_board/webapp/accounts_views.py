from django.shortcuts import render
from django.views.generic import View

from .helpers import accounts_helper;

class AccountsView(View):
    def get(self, request):
        accounts = accounts_helper.get_all_accounts(request)
        return render(request, 'accounts/accounts.html', {
            "accounts": accounts
        })