package su.aly.alysuchka.ui.test;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
public class TestFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("ПАНЕЛЬ 3");
        tv.setTextSize(18);
        tv.setPadding(32,32,32,32);
        return tv;
    }
}
