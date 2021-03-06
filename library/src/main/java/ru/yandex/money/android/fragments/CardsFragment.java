/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 NBCO Yandex.Money LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yandex.money.android.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.yandex.money.api.model.ExternalCard;

import java.math.BigDecimal;
import java.util.List;

import ru.yandex.money.android.R;
import ru.yandex.money.android.database.DatabaseStorage;
import ru.yandex.money.android.formatters.MoneySourceFormatter;
import ru.yandex.money.android.utils.CardType;
import ru.yandex.money.android.utils.Views;

/**
 * @author vyasevich
 */
public class CardsFragment extends PaymentFragment implements AdapterView.OnItemClickListener {

    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTRACT_AMOUNT = "contractAmount";

    private int orientation;
    private PopupMenu menu;

    public static CardsFragment newInstance(String title, BigDecimal contractAmount) {
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putString(KEY_CONTRACT_AMOUNT, contractAmount.toPlainString());

        CardsFragment fragment = new CardsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ym_cards_fragment, container, false);
        assert view != null : "view is null";

        Bundle args = getArguments();
        assert args != null : "specify proper arguments for CardsFragment";

        Views.setText(view, R.id.ym_payment_name, args.getString(KEY_TITLE));
        Views.setText(view, R.id.ym_payment_sum, getString(R.string.ym_cards_payment_sum_value,
                new BigDecimal(args.getString(KEY_CONTRACT_AMOUNT))));

        ListView list = (ListView) view.findViewById(android.R.id.list);
        list.setAdapter(new CardsAdapter());
        list.setOnItemClickListener(this);

        orientation = getResources()
                .getConfiguration()
                .orientation;

        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (orientation != newConfig.orientation && menu != null) {
            menu.dismiss();
        }
        orientation = newConfig.orientation;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ExternalCard moneySource = (ExternalCard) parent.getItemAtPosition(position);
        if (moneySource == null) {
            proceed();
        } else {
            showCsc(moneySource);
        }
    }

    private class CardsAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private final DatabaseStorage databaseStorage;

        public CardsAdapter() {
            inflater = LayoutInflater.from(getPaymentActivity());
            databaseStorage = new DatabaseStorage(getPaymentActivity());
        }

        @Override
        public int getCount() {
            return getSize() + 1;
        }

        @Override
        public Object getItem(int position) {
            return getCardAtPosition(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // this is a hack to keep footer height unchanged
            return position == getSize() ? getFooterView(parent) : getCardView(position, parent);
        }

        private View getCardView(int position, ViewGroup parent) {

            View root = inflater.inflate(R.layout.ym_card_item, parent, false);
            assert root != null : "unable to inflate layout in CardsAdapter";

            final ExternalCard moneySource = getCardAtPosition(position);
            final TextView panFragment = (TextView) root.findViewById(R.id.ym_pan_fragment);
            panFragment.setText(MoneySourceFormatter.formatPanFragment(moneySource.panFragment));
            panFragment.setCompoundDrawablesWithIntrinsicBounds(CardType.get(
                    moneySource.type).cardResId, 0, 0, 0);

            ImageButton button = (ImageButton) root.findViewById(R.id.ym_actions);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup(v, moneySource);
                }
            });

            return root;
        }

        private View getFooterView(ViewGroup parent) {
            return inflater.inflate(R.layout.ym_cards_footer, parent, false);
        }

        private List<ExternalCard> getCards() {
            return getPaymentActivity().getCards();
        }

        private int getSize() {
            return getCards().size();
        }

        private ExternalCard getCardAtPosition(int position) {
            List<ExternalCard> cards = getCards();
            return position == cards.size() ? null : cards.get(position);
        }

        private void showPopup(View v, ExternalCard moneySource) {
            menu = new PopupMenu(getPaymentActivity(), v);
            MenuInflater inflater = menu.getMenuInflater();
            inflater.inflate(R.menu.ym_card_actions, menu.getMenu());
            menu.setOnMenuItemClickListener(new MenuItemClickListener(moneySource));
            menu.show();
        }

        private void deleteCard(ExternalCard moneySource) {
            databaseStorage.deleteMoneySource(moneySource);
            getCards().remove(moneySource);
            notifyDataSetChanged();
        }

        private class MenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

            private final ExternalCard moneySource;

            public MenuItemClickListener(ExternalCard moneySource) {
                this.moneySource = moneySource;
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.ym_delete) {
                    deleteCard(moneySource);
                    menu = null;
                    return true;
                }
                return false;
            }
        }
    }
}
