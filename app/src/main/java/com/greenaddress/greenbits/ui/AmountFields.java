package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.math.BigDecimal;

/**
 * Created by Antonio Parrella on 11/16/16.
 * by inbitcoin
 */
class AmountFields {
    private final EditText mAmountEdit;
    private final EditText mAmountFiatEdit;
    private final FontAwesomeTextView mFiatView;
    private boolean mConverting;
    private final GaService mGaService;
    private final Context mContext;
    private Boolean mIsPausing = false;

    interface OnConversionFinishListener {
        void conversionFinish();
    }

    private final OnConversionFinishListener mOnConversionFinishListener;

    AmountFields(final GaService gaService, final Context context, final View view, final OnConversionFinishListener onConversionFinishListener) {
        mGaService = gaService;
        mContext = context;
        mOnConversionFinishListener = onConversionFinishListener;

        mAmountEdit = UI.find(view, R.id.sendAmountEditText);
        mAmountFiatEdit = UI.find(view, R.id.sendAmountFiatEditText);
        mFiatView = UI.find(view, R.id.sendFiatIcon);

        final TextView bitcoinUnitText = UI.find(view, R.id.sendBitcoinUnitText);
        UI.setCoinText(mGaService, bitcoinUnitText, null, null);

        mAmountFiatEdit.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertFiatToBtc();
            }
        });

        mAmountEdit.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (mGaService.hasFiatRate())
                    convertBtcToFiat();
            }
        });

        updateFiatFields();
    }

    private void updateFiatFields() {
        changeFiatIcon(mFiatView, mGaService.getFiatCurrency());

        if (!mGaService.hasFiatRate()) {
            // Disable fiat editing
            mAmountFiatEdit.setText("N/A");
            UI.disable(mAmountFiatEdit);
        } else {
            if (UI.getText(mAmountFiatEdit).equals("N/A"))
                convertBtcToFiat(); // Fiat setting changed, recalc it
        }
    }

    void setIsPausing(final Boolean isPausing) {
        mIsPausing = isPausing;
        if (!isPausing)
            updateFiatFields(); // Resuming: Update in case fiat changed in prefs
    }

    Boolean isPausing() {
        return mIsPausing;
    }

    public static void changeFiatIcon(final FontAwesomeTextView fiatIcon, final String currency) {
        final String symbol;
        switch (currency) {
            case "USD": symbol = "&#xf155; "; break;
            case "AUD": symbol = "&#xf155; "; break;
            case "CAD": symbol = "&#xf155; "; break;
            case "EUR": symbol = "&#xf153; "; break;
            case "CNY": symbol = "&#xf157; "; break;
            case "GBP": symbol = "&#xf154; "; break;
            case "ILS": symbol = "&#xf20b; "; break;
            case "RUB": symbol = "&#xf158; "; break;
            case "BRL": symbol = "R&#xf155; "; break;
            default:
                fiatIcon.setText(currency);
                fiatIcon.setDefaultTypeface();
                fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                return;
        }
        fiatIcon.setText(Html.fromHtml(symbol));
        fiatIcon.setAwesomeTypeface();
        fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
    }

    void convertBtcToFiat() {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;
        try {
            final Coin btcValue = UI.parseCoinValue(mGaService, UI.getText(mAmountEdit));
            Fiat fiatValue = mGaService.getFiatRate().coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            mAmountFiatEdit.setText(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            final String maxAmount = mContext.getString(R.string.send_max_amount);
            if (UI.getText(mAmountEdit).equals(maxAmount))
                mAmountFiatEdit.setText(maxAmount);
            else
                UI.clear(mAmountFiatEdit);
        }
        finishConversion();
    }

    private void convertFiatToBtc() {
        if (mConverting || mIsPausing)
            return;

        mConverting = true;
        try {
            final Fiat fiatValue = Fiat.parseFiat("???", UI.getText(mAmountFiatEdit));
            mAmountEdit.setText(UI.formatCoinValue(mGaService, mGaService.getFiatRate().fiatToCoin(fiatValue)));
        } catch (final ArithmeticException | IllegalArgumentException e) {
            UI.clear(mAmountEdit);
        }
        finishConversion();
    }

    private void finishConversion() {
        if (mOnConversionFinishListener != null)
            mOnConversionFinishListener.conversionFinish();
        mConverting = false;
    }
}
