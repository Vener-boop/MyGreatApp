package su.aly.alysuchka.ui.contacts;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
public class ContactsFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("Контакты");
        tv.setTextSize(18);
        tv.setPadding(32,32,32,32);
        return tv;
    }
}
