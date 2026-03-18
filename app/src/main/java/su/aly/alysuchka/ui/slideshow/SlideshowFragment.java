package su.aly.alysuchka.ui.slideshow;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import su.aly.alysuchka.R;
public class SlideshowFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slideshow, container, false);
    }
}
