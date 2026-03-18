package su.aly.alysuchka.ui.mudozvon;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import su.aly.alysuchka.R;

public class MudozvonFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 123;

    // Views
    private View rootView;
    private EditText editNumberPrefix, editNumberIndex, editNumberStart, editNumberEnd;
    private EditText editTimeBetweenCalls, editTimeAfterCalls;
    private EditText editTimeDayStart, editTimeDayEnd;
    private EditText editNumberForTest, editCurrentNumber;
    private CheckBox checkBoxTest, checkBoxDayTime, checkBoxCsvMode;
    private Button buttonStart, buttonStop, buttonLoadCsv;
    private TextView textLog, labelCsvFile, labelCsvCount;
    private ScrollView scrollViewLog;

    // Numeric mode state
    private String current_prefix = "";
    private String current_index = "";
    private String current_number = "";
    private long current_number_long;
    private long start_number_long;
    private long end_number_long;
    private long time_after_long = 3000;
    private long time_between_long = 4000;
    private String test_number = "";
    private boolean bool_test_call = false;
    private boolean bool_check_day_time = false;
    private boolean bool_caller_stop = true;
    private boolean bool_msg_waiting = false;

    // CSV mode state
    private boolean csvMode = false;
    private final List<String> csvPhones = new ArrayList<>();
    private int csvCurrentIndex = 0;

    // File picker
    private ActivityResultLauncher<String[]> csvPickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        csvPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) loadCsvFromUri(uri); }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_mudozvon, container, false);
        bindViews();
        loadSettings();
        checkPermissions();
        setupListeners();
        addLog("Приложение запущено. Готово к работе.");
        return rootView;
    }

    private void bindViews() {
        editNumberPrefix     = rootView.findViewById(R.id.editNumberPrefix);
        editNumberIndex      = rootView.findViewById(R.id.editNumberIndex);
        editNumberStart      = rootView.findViewById(R.id.editNumberStart);
        editNumberEnd        = rootView.findViewById(R.id.editNumberEnd);
        editTimeBetweenCalls = rootView.findViewById(R.id.editTimeBetweenCalls);
        editTimeAfterCalls   = rootView.findViewById(R.id.editTimeAfterCalls);
        editTimeDayStart     = rootView.findViewById(R.id.editTimeDayStart);
        editTimeDayEnd       = rootView.findViewById(R.id.editTimeDayEnd);
        editNumberForTest    = rootView.findViewById(R.id.editNumberForTest);
        editCurrentNumber    = rootView.findViewById(R.id.editCurrentNumber);
        checkBoxTest         = rootView.findViewById(R.id.checkBox_Test);
        checkBoxDayTime      = rootView.findViewById(R.id.checkBox_Day_Time);
        checkBoxCsvMode      = rootView.findViewById(R.id.checkBox_Csv_Mode);
        buttonStart          = rootView.findViewById(R.id.button_Start);
        buttonStop           = rootView.findViewById(R.id.button_Stop);
        buttonLoadCsv        = rootView.findViewById(R.id.button_Load_Csv);
        textLog              = rootView.findViewById(R.id.text_Log);
        scrollViewLog        = rootView.findViewById(R.id.scrollView_Log);
        labelCsvFile         = rootView.findViewById(R.id.label_Csv_File);
        labelCsvCount        = rootView.findViewById(R.id.label_Csv_Count);

        if (buttonStop != null) buttonStop.setEnabled(false);
    }

    private void setupListeners() {
        Context ctx = requireContext().getApplicationContext();

        if (buttonStart != null) buttonStart.setOnClickListener(v -> onStartClick(ctx));
        if (buttonStop  != null) buttonStop.setOnClickListener(v -> onStopClick());
        if (buttonLoadCsv != null) buttonLoadCsv.setOnClickListener(v -> openCsvPicker());

        if (checkBoxTest != null) {
            checkBoxTest.setOnClickListener(v -> {
                View panel = rootView.findViewById(R.id.panel_Container_For_Test);
                if (panel != null) panel.setVisibility(checkBoxTest.isChecked() ? View.VISIBLE : View.GONE);
            });
        }

        if (checkBoxCsvMode != null) {
            checkBoxCsvMode.setOnCheckedChangeListener((cb, checked) -> {
                csvMode = checked;
                View panelNum = rootView.findViewById(R.id.panel_Container_Start_Phone_Number);
                View panelCsv = rootView.findViewById(R.id.panel_Container_Csv);
                if (panelNum != null) panelNum.setVisibility(checked ? View.GONE : View.VISIBLE);
                if (panelCsv != null) panelCsv.setVisibility(checked ? View.VISIBLE : View.GONE);
            });
        }

        // Save on Done for each EditText
        setupSaveOnDone(editNumberPrefix,     ctx, "editNumberPrefix");
        setupSaveOnDone(editNumberIndex,      ctx, "editNumberIndex");
        setupSaveOnDone(editNumberStart,      ctx, "editNumberStart");
        setupSaveOnDone(editNumberEnd,        ctx, "editNumberEnd");
        setupSaveOnDone(editTimeBetweenCalls, ctx, "editTimeBetweenCalls");
        setupSaveOnDone(editTimeAfterCalls,   ctx, "editTimeAfterCalls");
        setupSaveOnDone(editTimeDayStart,     ctx, "editTimeDayStart");
        setupSaveOnDone(editTimeDayEnd,       ctx, "editTimeDayEnd");
        setupSaveOnDone(editNumberForTest,    ctx, "editNumberForTest");
    }

    private void setupSaveOnDone(EditText et, Context ctx, String key) {
        if (et == null) return;
        et.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                setIni(ctx, key, et.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    // ─── CSV ─────────────────────────────────────────────────────────────────

    private void openCsvPicker() {
        csvPickerLauncher.launch(new String[]{"text/csv", "text/comma-separated-values", "text/plain", "*/*"});
    }

    private void loadCsvFromUri(Uri uri) {
        csvPhones.clear();
        csvCurrentIndex = 0;
        int loaded = 0;
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) { addLog("Не удалось открыть файл"); return; }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (first) { first = false; if (isHeader(line)) continue; }
                String phone = extractPhone(line);
                if (phone != null && phone.length() >= 10) {
                    csvPhones.add(phone);
                    loaded++;
                }
            }
            reader.close();

            String fileName = getFileName(uri);
            if (labelCsvFile  != null) labelCsvFile.setText("Файл: " + fileName);
            if (labelCsvCount != null) labelCsvCount.setText("Номеров: " + loaded);
            addLog("CSV загружен: " + fileName + " — " + loaded + " номеров");
            Toast.makeText(requireContext(), "Загружено: " + loaded + " номеров", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            addLog("Ошибка загрузки CSV: " + e.getMessage());
        }
    }

    private boolean isHeader(String line) {
        String l = line.toLowerCase();
        return l.contains("фио") || l.contains("имя") || l.contains("name") ||
               l.contains("телефон") || l.contains("phone") || l.contains("контакт") ||
               l.contains("номер");
    }

    private String extractPhone(String line) {
        String[] parts = line.contains(";") ? line.split(";") : line.split(",");
        for (String p : parts) {
            String clean = cleanPhone(p.trim().replace("\"", ""));
            if (clean.length() >= 10) return clean;
        }
        return null;
    }

    private String cleanPhone(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        boolean hasPlus = raw.trim().startsWith("+");
        String digits = raw.replaceAll("[^0-9]", "");
        if (hasPlus) digits = "+" + digits;
        // 8XXXXXXXXXX → +7XXXXXXXXXX
        if (!hasPlus && digits.startsWith("8") && digits.length() == 11)
            digits = "+7" + digits.substring(1);
        // 7XXXXXXXXXX → +7XXXXXXXXXX
        if (!hasPlus && digits.startsWith("7") && digits.length() == 11)
            digits = "+" + digits;
        return digits;
    }

    private String getFileName(Uri uri) {
        try (android.database.Cursor c = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {}
        return uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "файл";
    }

    // ─── СТАРТ / СТОП ────────────────────────────────────────────────────────

    private void onStartClick(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CALL_PHONE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            addLog("Нет разрешения CALL_PHONE. Запрашиваю...");
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CODE);
            return;
        }

        if (csvMode) {
            if (csvPhones.isEmpty()) {
                Toast.makeText(requireContext(), "Сначала загрузите CSV!", Toast.LENGTH_SHORT).show();
                addLog("CSV не загружен или пустой!");
                return;
            }
            csvCurrentIndex = 0;
        } else {
            try {
                start_number_long = Long.parseLong(editNumberStart.getText().toString().trim());
                end_number_long   = Long.parseLong(editNumberEnd.getText().toString().trim());
            } catch (NumberFormatException e) {
                addLog("Ошибка: неверный формат номера!");
                return;
            }
            if (end_number_long < start_number_long) {
                addLog("Конечный номер меньше начального!");
                return;
            }
            current_prefix      = editNumberPrefix.getText().toString().trim();
            current_index       = editNumberIndex.getText().toString().trim();
            current_number      = addZero(String.valueOf(start_number_long));
            current_number_long = start_number_long;
        }

        try { time_between_long = Long.parseLong(editTimeBetweenCalls.getText().toString().trim()); }
        catch (Exception e) { time_between_long = 4000; }
        try { time_after_long = Long.parseLong(editTimeAfterCalls.getText().toString().trim()); }
        catch (Exception e) { time_after_long = 3000; }

        test_number         = editNumberForTest.getText().toString().trim();
        bool_test_call      = checkBoxTest.isChecked();
        bool_check_day_time = checkBoxDayTime.isChecked();
        bool_caller_stop    = false;
        bool_msg_waiting    = false;

        addLog("=== Старт автообзвона ===");
        if (buttonStart != null) buttonStart.setEnabled(false);
        if (buttonStop  != null) buttonStop.setEnabled(true);

        runCall(ctx);
    }

    private void onStopClick() {
        if (!bool_caller_stop) {
            bool_caller_stop = true;
            if (buttonStart != null) buttonStart.setEnabled(true);
            if (buttonStop  != null) buttonStop.setEnabled(false);
            addLog("=== Остановка автообзвона ===");
        }
    }

    // ─── ЛОГИКА ЗВОНКА ───────────────────────────────────────────────────────

    private void runCall(Context ctx) {
        if (bool_caller_stop) return;

        // Проверка временного диапазона
        if (bool_check_day_time && !isTimeInRange()) {
            if (!bool_msg_waiting) {
                addLog("Вне временного диапазона. Ожидание...");
                bool_msg_waiting = true;
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> runCall(ctx), 30_000);
            return;
        }
        bool_msg_waiting = false;

        // Определяем номер
        String numberToDial;
        if (csvMode) {
            if (csvCurrentIndex >= csvPhones.size()) {
                addLog("CSV обзвон завершён. Все " + csvPhones.size() + " номеров обработаны.");
                finishCalling();
                return;
            }
            numberToDial = csvPhones.get(csvCurrentIndex);
        } else {
            if (current_number_long > end_number_long) {
                addLog("Диапазон завершён. Обзвон остановлен.");
                finishCalling();
                return;
            }
            numberToDial = current_prefix + current_index + current_number;
        }

        String dialNumber = bool_test_call ? test_number : numberToDial;

        if (editCurrentNumber != null) editCurrentNumber.setText(dialNumber);
        addLog("Вызов → " + dialNumber);

        // Набор номера
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + dialNumber));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            addLog("Ошибка вызова: " + e.getMessage());
            scheduleNextCall(ctx);
            return;
        }

        // Через time_after_long мс — сбросить
        new Handler(Looper.getMainLooper()).postDelayed(() -> endCall(ctx), time_after_long);
    }

    private void endCall(Context ctx) {
        if (bool_caller_stop) return;

        addLog("Сброс вызова...");
        boolean hung = false;

        // Метод 1: TelecomManager.endCall()
        try {
            TelecomManager tm = (TelecomManager) ctx.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null) {
                ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.ANSWER_PHONE_CALLS);
                tm.endCall();
                addLog("Сброс OK (TelecomManager)");
                hung = true;
            }
        } catch (Exception e) {
            addLog("TelecomManager fail: " + e.getMessage());
        }

        // Метод 2: ITelephony через reflection (запасной)
        if (!hung) {
            try {
                android.telephony.TelephonyManager telephony =
                    (android.telephony.TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephony != null) {
                    java.lang.reflect.Method method = telephony.getClass()
                        .getDeclaredMethod("getITelephony");
                    method.setAccessible(true);
                    Object iTelephony = method.invoke(telephony);
                    if (iTelephony != null) {
                        iTelephony.getClass().getDeclaredMethod("endCall").invoke(iTelephony);
                        addLog("Сброс OK (ITelephony reflection)");
                        hung = true;
                    }
                }
            } catch (Exception e) {
                addLog("ITelephony fail: " + e.getMessage());
            }
        }

        if (!hung) addLog("Не удалось сбросить. Переходим к следующему.");

        scheduleNextCall(ctx);
    }

    private void scheduleNextCall(Context ctx) {
        if (bool_caller_stop) return;

        // Переходим к следующему номеру
        if (csvMode) {
            csvCurrentIndex++;
            if (csvCurrentIndex >= csvPhones.size()) {
                addLog("CSV обзвон завершён.");
                finishCalling();
                return;
            }
        } else {
            current_number_long++;
            current_number = addZero(String.valueOf(current_number_long));
            if (editNumberStart != null) {
                editNumberStart.setText(current_number);
                setIni(ctx, "editNumberStart", current_number);
            }
            if (current_number_long > end_number_long) {
                addLog("Диапазон завершён.");
                finishCalling();
                return;
            }
        }

        // Ждём time_between_long перед следующим звонком
        new Handler(Looper.getMainLooper()).postDelayed(() -> runCall(ctx), time_between_long);
    }

    private void finishCalling() {
        bool_caller_stop = true;
        if (buttonStart != null) buttonStart.setEnabled(true);
        if (buttonStop  != null) buttonStop.setEnabled(false);
        addLog("=== Обзвон завершён ===");
    }

    // ─── ВСПОМОГАТЕЛЬНЫЕ ─────────────────────────────────────────────────────

    private String addZero(String s) {
        while (s.length() < 7) s = "0" + s;
        return s;
    }

    private void addLog(String msg) {
        if (textLog == null) return;
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String prev = textLog.getText().toString();
        textLog.setText(time + " : " + msg + (prev.isEmpty() ? "" : "\n" + prev));
        if (scrollViewLog != null) scrollViewLog.post(() -> scrollViewLog.fullScroll(View.FOCUS_UP));
    }

    private boolean isTimeInRange() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String now   = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            String start = editTimeDayStart.getText().toString().trim();
            String end   = editTimeDayEnd.getText().toString().trim();
            Date dNow    = sdf.parse(now);
            Date dStart  = sdf.parse(start);
            Date dEnd    = sdf.parse(end);
            if (dNow == null || dStart == null || dEnd == null) return true;
            return !dNow.before(dStart) && !dNow.after(dEnd);
        } catch (Exception e) { return true; }
    }

    private void hideKeyboard() {
        View focused = rootView != null ? rootView.findFocus() : null;
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void checkPermissions() {
        Context ctx = requireContext();
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CALL_PHONE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED)
            needed.add(android.Manifest.permission.CALL_PHONE);
        if (ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.ANSWER_PHONE_CALLS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED)
            needed.add(android.Manifest.permission.ANSWER_PHONE_CALLS);

        if (!needed.isEmpty()) {
            addLog("Запрос разрешений...");
            requestPermissions(needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            addLog("Все разрешения получены.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERMISSION_REQUEST_CODE) {
            boolean allOk = true;
            for (int r : results) if (r != android.content.pm.PackageManager.PERMISSION_GRANTED) allOk = false;
            addLog(allOk ? "Разрешения получены." : "Некоторые разрешения отклонены.");
        }
    }

    // ─── SharedPreferences ───────────────────────────────────────────────────

    private void loadSettings() {
        Context ctx = requireContext().getApplicationContext();
        if (editNumberPrefix     != null) editNumberPrefix.setText(getIni(ctx, "editNumberPrefix", "+7"));
        if (editNumberIndex      != null) editNumberIndex.setText(getIni(ctx, "editNumberIndex", "982"));
        if (editNumberStart      != null) editNumberStart.setText(getIni(ctx, "editNumberStart", "0000000"));
        if (editNumberEnd        != null) editNumberEnd.setText(getIni(ctx, "editNumberEnd", "9999999"));
        if (editTimeBetweenCalls != null) editTimeBetweenCalls.setText(getIni(ctx, "editTimeBetweenCalls", "4000"));
        if (editTimeAfterCalls   != null) editTimeAfterCalls.setText(getIni(ctx, "editTimeAfterCalls", "3000"));
        if (editTimeDayStart     != null) editTimeDayStart.setText(getIni(ctx, "editTimeDayStart", "08:00"));
        if (editTimeDayEnd       != null) editTimeDayEnd.setText(getIni(ctx, "editTimeDayEnd", "22:00"));
        if (editNumberForTest    != null) editNumberForTest.setText(getIni(ctx, "editNumberForTest", "+79826326666"));
    }

    private String getIni(Context ctx, String key, String def) {
        return ctx.getSharedPreferences("ini_alysuchka", Context.MODE_PRIVATE).getString(key, def);
    }

    private void setIni(Context ctx, String key, String value) {
        ctx.getSharedPreferences("ini_alysuchka", Context.MODE_PRIVATE)
           .edit().putString(key, value).apply();
    }
}
