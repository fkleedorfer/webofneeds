package won.utils.blend.support.stats.comparator;

import won.utils.blend.support.stats.GroupedTemplateStats;

import java.util.Comparator;

public class GroupedTemplateStatsComparator implements Comparator<GroupedTemplateStats> {
    @Override
    public int compare(GroupedTemplateStats o1, GroupedTemplateStats o2) {
        int cmp = 0;
        cmp = o2.bindingStats.unboundCount - o1.bindingStats.unboundCount;
        if (cmp != 0) {
            return cmp;
        }
        cmp = o2.bindingStats.boundToVariableCount - o1.bindingStats.boundToVariableCount;
        if (cmp != 0) {
            return cmp;
        }
        cmp = o2.bindingStats.boundToConstantCount - o1.bindingStats.boundToConstantCount;
        if (cmp != 0) {
            return cmp;
        }
        cmp = o2.count - o1.count;
        return cmp;
    }
}
