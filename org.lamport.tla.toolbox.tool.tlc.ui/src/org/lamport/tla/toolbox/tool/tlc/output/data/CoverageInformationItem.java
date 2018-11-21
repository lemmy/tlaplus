package org.lamport.tla.toolbox.tool.tlc.output.data;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.lamport.tla.toolbox.tool.tlc.ui.util.IModuleLocatable;

import tla2sany.st.Location;
import tlc2.TLCGlobals;

/**
 * Coverage information item
 * @author Simon Zambrovski
 */
public class CoverageInformationItem implements IModuleLocatable
{
    private final static String MOD = " of module ";
    private final static String COLON = ": ";
    private final static String AT = "at ";

    private String locationString;
    protected Location location;
    protected String modelName;
    protected long count;
    protected int layer;

    /**
     * Creates an simple item storing information about a coverage at a certain location
     * @param location
     * @param count
     * @param modelName the name of the model for which this is coverage information
     * @param module
     */

    protected CoverageInformationItem(Location location, long count, String modelName, int layer)
    {
        this.location = location;
        this.locationString = this.location.toString();
        this.count = count;
        this.modelName = modelName;
        this.layer = layer;
    }

    public CoverageInformationItem() {
	}

	public final String getModule()
    {
        return locationString.substring(locationString.indexOf(MOD) + MOD.length());
    }

    public final String getLocation()
    {
        return locationString.substring(0, locationString.indexOf(MOD));
    }

    public final long getCount()
    {
        return count;
    }

	/**
	 * If two CCI are co-located (overlapping, nested, ...), the layer indicates
	 * which one is considered more important.
	 */
	public int getLayer() {
		return layer;
	}

    /**
     * Parses the coverage information item from a string
     * @param outputMessage
     * @param modelName the name of the model for which this is coverage information
     * @return
     */
    public static CoverageInformationItem parse(String outputMessage, String modelName)
    {

        // "  line 84, col 32 to line 85, col 73 of module AtomicBakery: 1012492"
        outputMessage = outputMessage.trim();
        
        final int layer = outputMessage.lastIndexOf(TLCGlobals.coverageIndent) + 1;
        
        int index = outputMessage.indexOf(COLON);
        return new CoverageInformationItem(Location.parseLocation(outputMessage.substring(layer, index)), Long
                .parseLong(outputMessage.substring(index + COLON.length())), modelName, layer);
    }

    /**
     * Parses coverage timestamp from the string  
     * @param outputMessage
     * @return
     */
    public static String parseCoverageTimestamp(String outputMessage)
    {
        return outputMessage.substring(outputMessage.lastIndexOf(AT) + AT.length());
    }

    /**
     * The {@link Location} in the module.
     * @return
     */
    public Location getModuleLocation()
    {
        return location;
    }

    /**
     * The name of the model.
     * 
     * @return
     */
    public String getModelName()
    {
        return modelName;
    }

    private final List<CoverageInformationItem> siblings = new ArrayList<>();
    
	CoverageInformationItem addSiblings(List<CoverageInformationItem> siblings) {
		this.siblings.addAll(siblings);
		this.siblings.remove(this);
		return this;
	}

    private final List<CoverageInformationItem> childs = new ArrayList<>();
    
	List<CoverageInformationItem> getChildren() {
		return childs;
	}

	CoverageInformationItem addChild(CoverageInformationItem child) {
		this.childs.add(child);
		return this;
	}

	CoverageInformationItem setLayer(int i) {
		this.layer = i;
		return this;
	}
	
	private Color color, aggregateColor;

	protected CoverageInformationItem setColor(Color c, Color a) {
		this.color = c;
		this.aggregateColor = a == null ? c : a;
		return this;
	}

	private IRegion region;

	IRegion getRegion() {
		return this.region;
	}

	CoverageInformationItem setRegion(IRegion locationToRegion) {
		this.region = locationToRegion;
		return this;
	}

	private boolean isRoot() {
		return layer == -1;
	}

	private boolean active = false;
	
	public boolean isActive() {
		return active;
	}

	protected StyleRange addStlye(StyleRange sr) {
		// no-op
		return sr;
	}

	public void style(final TextPresentation textPresentation) {
		if (isRoot()) {
			style(textPresentation, true);
		} else {
			style(textPresentation, false);
		}
	}
	
	protected void style(final TextPresentation textPresentation, boolean merge) {
		if (!isRoot()) {
			final StyleRange rs = new StyleRange();
			
			// IRegion
			rs.start = region.getOffset();
			rs.length = region.getLength();
			
			// Background Color
			if (merge) {
				rs.background = aggregateColor;
			} else {
				rs.background = color;
			}
			
			// Zero Coverage
			if (getCount() == 0L) {
				rs.background = null;
				rs.borderStyle = SWT.BORDER_SOLID;
				rs.borderColor = JFaceResources.getColorRegistry().get(CoverageInformation.RED);
			}
			
			// Track active subtree
			rs.data = this; //mergeStyleRange does not merge rs.data, thus track active instead.
			active = true;
			
			textPresentation.mergeStyleRange(addStlye(rs));
		}
		for (CoverageInformationItem child : childs) {
			child.style(textPresentation, merge);
		}
	}

	public void style(final TextPresentation textPresentation, final Color c) {
		if (!isRoot()) {
			final StyleRange rs = new StyleRange();
			rs.start = region.getOffset();
			rs.length = region.getLength();
			rs.background = c;
			if (getCount() == 0L) {
				rs.background = null;
				rs.borderStyle = SWT.BORDER_SOLID;
				rs.borderColor = JFaceResources.getColorRegistry().get(CoverageInformation.RED);
			}
			active = false;
			textPresentation.replaceStyleRange(addStlye(rs));
		}
		for (CoverageInformationItem child : childs) {
			child.style(textPresentation, c);
		}
	}

	void colorItem(TreeSet<Long> counts, final int numSiblings) {
		int hue = CoverageInformation.getHue(getCount(), counts);
		String key = Integer.toString(hue);
		if (!JFaceResources.getColorRegistry().hasValueFor(key)) {
			JFaceResources.getColorRegistry().put(key, new RGB(hue, .25f, 1f));
		}
		Color color = JFaceResources.getColorRegistry().get(key);
		
		// Aggregated color (might be identical to color).
		hue = CoverageInformation.getHue(getCount() * numSiblings, counts);
		key = Integer.toString(hue);
		if (!JFaceResources.getColorRegistry().hasValueFor(key)) {
			JFaceResources.getColorRegistry().put(key, new RGB(hue, .25f, 1f));
		}
		Color aggregate = JFaceResources.getColorRegistry().get(key);
		
		setColor(color, aggregate);
	}
}
