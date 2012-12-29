package net.oddsoftware.android.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.oddsoftware.android.feedscribe.Globals;

import org.htmlcleaner.BrowserCompactXmlSerializer;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CleanerTransformations;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.DoctypeToken;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.htmlcleaner.TagTransformation;
import org.htmlcleaner.XPatherException;

import android.util.Log;

public class Textify
{
    protected String mTitle = null;
    
    private String mAuthor = null;

    private String mPubDate = null;
    
    protected HashMap<TagNode, Integer> mTagScores;
    protected HashMap<String, HashSet<String> > mAllowedAttributes;
    protected HashSet<String > mBlacklistTags;
    protected HashMap<String, String> mConvertTags;
    
    protected Pattern mUnlikelyCandidates;
    protected Pattern mOkMaybeItsACandidates;
    protected Pattern mDivToPElements;
    protected Pattern mPositiveClassNames;
    protected Pattern mNegativeClassNames;
    protected Pattern mSentencePattern;
    
    protected HtmlCleaner mCleaner;
    
    protected TagNode mArticleRoot;
    
    protected boolean mProcessingEnabled;

    private boolean mStripUnlikelyCandidates = true;

    private int mBestScore;
    
    private boolean mForcePageWidth = false;
    
    private String mViewport = null;
    
    public Textify()
    {
        mUnlikelyCandidates = Pattern.compile("combx|comment|community|disqus|extra|foot|header|menu|remark|rss|" +
                "shoutbox|sidebar|sponsor|ad-break|agegate|pagination|pager|popup|tweet|twitter",
                Pattern.CASE_INSENSITIVE);
        
        // header is in here for espnf1 and it should be fine since the header will be boring by other means
        mOkMaybeItsACandidates = Pattern.compile("and|article|body|column|main|shadow|commentary-panel|header",
                Pattern.CASE_INSENSITIVE);
        
        mDivToPElements = Pattern.compile("a|blockquote|dl|div|img|ol|p|pre|table|ul",
                Pattern.CASE_INSENSITIVE);
        
        mPositiveClassNames = Pattern.compile("article|body|content|entry|hentry|main|page|pagination|post|text|blog|story|stry|" +
        		"datetools",
                Pattern.CASE_INSENSITIVE);
        
        mNegativeClassNames = Pattern.compile("combx|comment|com-|contact|foot|footer|footnote|masthead|media|meta|outbrain|" +
                "promo|related|scroll|shoutbox|sidebar|sponsor|shopping|tags|tool|widget|contentheading",
                Pattern.CASE_INSENSITIVE);
        
        mSentencePattern = Pattern.compile("\\.( |$)");
        
        
        mAllowedAttributes = new HashMap<String, HashSet<String> >();
        
        HashSet<String> emptySet = new HashSet<String>();
        
        mAllowedAttributes.put("p", emptySet );
        mAllowedAttributes.put("div", emptySet );
        mAllowedAttributes.put("br", emptySet );
        mAllowedAttributes.put("b", emptySet );
        mAllowedAttributes.put("big", emptySet );
        mAllowedAttributes.put("center", emptySet );
        mAllowedAttributes.put("code", emptySet );
        mAllowedAttributes.put("cite", emptySet );
        mAllowedAttributes.put("del", emptySet );
        mAllowedAttributes.put("dfn", emptySet );
        mAllowedAttributes.put("em", emptySet );
        mAllowedAttributes.put("font", emptySet ); // color, face, size
        mAllowedAttributes.put("u", emptySet );
        mAllowedAttributes.put("i", emptySet );
        mAllowedAttributes.put("ins", emptySet );
        mAllowedAttributes.put("kbd", emptySet );
        mAllowedAttributes.put("pre", emptySet );
        mAllowedAttributes.put("s", emptySet );
        mAllowedAttributes.put("samp", emptySet );
        mAllowedAttributes.put("small", emptySet );
        mAllowedAttributes.put("strong", emptySet );
        mAllowedAttributes.put("sub", emptySet );
        mAllowedAttributes.put("sup", emptySet );
        mAllowedAttributes.put("span", emptySet );
        mAllowedAttributes.put("strike", emptySet );
        mAllowedAttributes.put("tt", emptySet );
        mAllowedAttributes.put("var", emptySet );
        mAllowedAttributes.put("abbr", emptySet );
        mAllowedAttributes.put("acronym", emptySet );
        mAllowedAttributes.put("address", emptySet );
        mAllowedAttributes.put("blockquote", emptySet );
        mAllowedAttributes.put("q", emptySet );
        mAllowedAttributes.put("wbr", emptySet );
        mAllowedAttributes.put("nobr", emptySet );
        mAllowedAttributes.put("xmp", emptySet );
        mAllowedAttributes.put("hr", emptySet );
        
        mAllowedAttributes.put("th", emptySet );
        mAllowedAttributes.put("td", emptySet );
        mAllowedAttributes.put("tr", emptySet );
        mAllowedAttributes.put("thead", emptySet );
        mAllowedAttributes.put("tbody", emptySet );
        mAllowedAttributes.put("tfoot", emptySet );
        mAllowedAttributes.put("col", emptySet );
        mAllowedAttributes.put("colgroup", emptySet );
        mAllowedAttributes.put("caption", emptySet );
        
        mAllowedAttributes.put("li", emptySet );
        mAllowedAttributes.put("ul", emptySet );
        mAllowedAttributes.put("ol", emptySet );
        mAllowedAttributes.put("dd", emptySet );
        mAllowedAttributes.put("dl", emptySet );
        mAllowedAttributes.put("dt", emptySet );
        mAllowedAttributes.put("menu", emptySet );
        mAllowedAttributes.put("dir", emptySet );
        
        mAllowedAttributes.put("a", new HashSet<String>(Arrays.asList("href") ) );
        mAllowedAttributes.put("img", new HashSet<String>(Arrays.asList("alt", "src", "title", "width", "height", "align", "usemap") ) );
        mAllowedAttributes.put("bdo", new HashSet<String>(Arrays.asList( "dir" ) ) );
        mAllowedAttributes.put("map", new HashSet<String>(Arrays.asList("name") ) );
        mAllowedAttributes.put("area", new HashSet<String>(Arrays.asList("shape", "coords", "href", "alt") ) );
        
        mAllowedAttributes.put("table", new HashSet<String>(Arrays.asList( "width", "cellspacing", "cellpadding", "border", "align" ) ) );
        
        mConvertTags = new HashMap<String, String>();
        mConvertTags.put( "blink", "span" );
        mConvertTags.put( "marquee", "span" );
        
        mBlacklistTags = new HashSet<String>();
        mBlacklistTags.add( "meta" );
        mBlacklistTags.add( "link" );
        mBlacklistTags.add( "style" );
        mBlacklistTags.add( "bgsound" );
        mBlacklistTags.add( "base" );
        mBlacklistTags.add( "object" );
        mBlacklistTags.add( "applet" );
        mBlacklistTags.add( "param" );
        mBlacklistTags.add( "script" );
        mBlacklistTags.add( "noscript" );
        mBlacklistTags.add( "basefont" );
        mBlacklistTags.add( "comment" );
        mBlacklistTags.add( "server" );
        mBlacklistTags.add( "iframe" );
        mBlacklistTags.add( "embed" );
        
        
        mBlacklistTags.add( "form" );
        mBlacklistTags.add( "input" );
        mBlacklistTags.add( "option" );
        mBlacklistTags.add( "textarea" );
        mBlacklistTags.add( "select" );
        mBlacklistTags.add( "optgroup" );
        mBlacklistTags.add( "button" );
        mBlacklistTags.add( "label" );
        mBlacklistTags.add( "fieldset" );
        mBlacklistTags.add( "legend" );
        mBlacklistTags.add( "isindex" );
        
        
        mCleaner = new HtmlCleaner();
        
        CleanerProperties props = mCleaner.getProperties();
        props.setOmitComments(true);
        props.setOmitDoctypeDeclaration(false);
        props.setTransResCharsToNCR(true);
        props.setPruneTags("script");
        props.setUseEmptyElementTags(false);
        
        CleanerTransformations transformations = new CleanerTransformations();
        transformations.addTransformation( new TagTransformation("noscript", "div") );
        mCleaner.setTransformations(transformations);
        
        mProcessingEnabled = true;
    }
    
    public void setProcessingEnabled(boolean processingEnabled)
    {
        mProcessingEnabled = processingEnabled;
    }
    
    public void setStripUnlikelyCandidates(boolean stripUnlikelyCandidates)
    {
        mStripUnlikelyCandidates = stripUnlikelyCandidates;
    }
    
    public void setViewport(String viewport)
    {
        mViewport = viewport;
    }
    
    public int getArticleScore()
    {
        return mBestScore;
    }
    
    public void process(String input)
    {
        TagNode root = mCleaner.clean( input );
        
        mBestScore = 0;
        mTagScores = new HashMap<TagNode, Integer>();
        
        if( mTitle == null )
        {
            mTitle = getArticleTitle(  root );
        }
        
        if( mProcessingEnabled )
        {
            preprocess(root);
            
            TagNode articleRoot = getArticleRoot( root );
            
            // System.out.println("<div>" + mCleaner.getInnerHtml( articleRoot ) + "</div>" );
            
            cleanArticle( articleRoot );
            
            // System.out.println("<div>" + mCleaner.getInnerHtml( articleRoot ) + "</div>" );
            
            mArticleRoot = articleRoot;
        }
        else
        {
            mArticleRoot = root.findElementByName("body", false);
            if(mArticleRoot == null)
            {
                mArticleRoot = root;
            }
        }
    }
    
    
    public void process(InputStream input) throws IOException
    {
        TagNode root = mCleaner.clean( input );
        
        mTagScores = new HashMap<TagNode, Integer>();
        
        if( mTitle == null )
        {
            mTitle = getArticleTitle(  root );
        }
        
        if( mProcessingEnabled )
        {
            
            preprocess(root);
            
            TagNode articleRoot = getArticleRoot( root );
            
            // System.out.println("<div>" + mCleaner.getInnerHtml( articleRoot ) + "</div>" );
            
            cleanArticle( articleRoot );
            
            // System.out.println("<div>" + mCleaner.getInnerHtml( articleRoot ) + "</div>" );
            
            mArticleRoot = articleRoot;
        }
        else
        {
            mArticleRoot = root.findElementByName("body", false);
            if(mArticleRoot == null)
            {
                mArticleRoot = root;
            }
        }
    }
    
    public void setTitle(String title)
    {
        mTitle = title;
    }
    
    public void setAuthor(String author)
    {
        mAuthor = author;
    }
    
    public void setPubDate(String pubDate)
    {
        mPubDate = pubDate;
    }
    
    public String getTitle()
    {
        return mTitle;
    }
    
    
    /*
    private ArrayList<TagNode> findNextPageLink()
    {
        return null;
    }
    
    private String findBaseUrl(String url)
    {
        return url;
    }
    */
    
    private double getLinkDensity(TagNode node)
    {
        TagNode[] links = node.getElementsByName("a", true);
        int textLength = countChars(node.getText());
        int linkLength = 0;
        
        for( int i = 0; i < links.length; ++i )
        {
            linkLength += countChars(links[i].getText());
        }
        
        if( textLength == 0 && links.length > 0 )
        {
            return 1.0;
        }
        else if (links.length == 0 )
        {
            return 0.0;
        }
        else
        {
            return linkLength / (double) textLength;
        }
    }
    
    private int countChars(StringBuffer buffer)
    {
        int count = 0;
        for( int i = 0; i < buffer.length(); ++i)
        {
            if(! Character.isWhitespace( buffer.charAt(i)))
            {
                ++count;
            }
        }
        return count;
    }
    
    /**
     * 
     * Find the article title node and process it
     * then find the first h1 and process it
     * 
     * @param root
     * @return
     */
    private String getArticleTitle( TagNode root )
    {
        String title = "";
        String originalTitle = "";
        
        Pattern p2 = Pattern.compile(":(.*)");
        
        // find the <title> tag and process it
        try
        {
            Object[] titles = root.evaluateXPath("/head/title");
            
            if( titles.length > 0 && titles[0] instanceof TagNode)
            {
                title = ((TagNode)titles[0]).getText().toString();
                originalTitle = title;
            }
        }
        catch(XPatherException exc)
        {
            if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "getArticleTitle:", exc);
        }
        
        // see if there is any "arttitle" tag - wtf is this ?
        TagNode artTitle = root.findElementByName("arttitle", true);
        
        // see if title is of the form "title - fred news" or "title | joe news"
        String[] parts = title.split("\\s+[|-]");
        // find the longest part
        String longest = null;
        for( int i = 0; i < parts.length; ++i)
        {
            if( longest == null || parts[i].length() > longest.length())
            {
                longest = parts[i];
            }
        }
        
        // override the above in the case of only 2 parts, use the first one
        if( parts.length == 2)
        {
            longest = parts[0];
        }
        
        // System.out.println("looking for title, have " + title + " and " + originalTitle + " and " + longest + " of " + parts.length);
        
        Matcher matcher;
        
        if( artTitle != null )
        {
            title = artTitle.getText().toString();
        }
        else if( parts.length >= 1 && longest != null )
        {
            title = longest;
        }
        // see if title is of the form "foo news: title"
        else if( (matcher = p2.matcher(title)).find() )
        {
            title = matcher.group(1);
        }
        else
        {
            // System.out.println("no title match");
        }
        
        // check title length, then try and find the one and only h1
        if( title.length() > 150 || title.length() < 15 )
        {
            // System.out.println("title '" + title + "' is of wrong size " + title.length());
            
            TagNode[] h1s = root.getElementsByName("h1", true);
            if( h1s.length == 1)
            {
                title = h1s[0].getText().toString();
                // System.out.println("replacing title title of wrong size with '" + title + "'");
            }
        }
        
        title = title.trim();
        
        // if it has less than 4 words, use the original, unprocessed title
        if( title.split(" ").length < 4)
        {
            // System.out.println(" title '" + title + "' is too small, using '" + originalTitle + "' instead" );
            title = originalTitle;
        }
        
        return title;
    }
    
    private int getClassWeight(TagNode node)
    {
        int weight = 0;
        
        String nodeClass = node.getAttributeByName("class");
        String nodeId = node.getAttributeByName("id");
        
        // Look for a special classname
        if( nodeClass != null )
        {
            if( mNegativeClassNames.matcher( nodeClass ).find() )
            {
                weight -= 25;
            }

            if( mPositiveClassNames.matcher( nodeClass ).find() )
            {
                weight += 25;
            }
        }
        
        if( nodeId != null )
        {
            if( mNegativeClassNames.matcher( nodeId ).find() )
            {
                weight -= 25;
            }

            if( mPositiveClassNames.matcher( nodeId ).find() )
            {
                weight += 25;
            }
        }

        return weight;

    }
    
    private void addScore(TagNode node, int score)
    {
        Integer integer = mTagScores.get(node);
        
        if( integer != null )
        {
            score += integer;
        }
        else
        {
            String name = node.getName();
            
            if( name.equals("div") )
            {
                score += 5;
            }
            else if (
                    name.equals("pre") ||
                    name.equals("td") ||
                    name.equals("blockquote")
                    )
            {
                score += 3;
            }
            else if (
                    name.equals("address") ||
                    name.equals("ol")      ||
                    name.equals("ul")      ||
                    name.equals("dl")      ||
                    name.equals("dd")      ||
                    name.equals("dt")      ||
                    name.equals("li")      ||
                    name.equals("form")
                    )
            {
                score += -3;
            }
            else if (
                    name.equals("th") ||
                    name.equals("h1") ||
                    name.equals("h2") ||
                    name.equals("h3") ||
                    name.equals("h4") ||
                    name.equals("h5") ||
                    name.equals("h6")
                    )
            {
                score += -5;
            }
            
            score += getClassWeight(node);
        }
        
        mTagScores.put(node, score);
    }
    
    
    private TagNode getArticleRoot( TagNode root )
    {
        TagNode[] allElements = root.getAllElements(true);
        ArrayList<TagNode> nodesToScore = new ArrayList<TagNode>();
        
        for( int i = 0; i < allElements.length; ++i)
        {
            TagNode currentNode = allElements[i];
            String tagName = currentNode.getName();
            
            if( mStripUnlikelyCandidates   )
            {
                String unlikelyMatchString = "" + currentNode.getAttributeByName("id") + currentNode.getAttributeByName("class");
                
                // see if we are still in the tree
                TagNode parent = currentNode;
                while( parent != null && parent != root )
                {
                    parent = parent.getParent();
                }
                if( parent != root )
                {
                    continue;
                }
                
                
                // String unlikelyMatchString = "" + currentNode.getAttributeByName("id") + currentNode.getAttributeByName("class");
                
                // System.out.println("getArticleRoot processing " + tagName + " " + unlikelyMatchString + " " + currentNode.getAttributeByName("style"));
                
                // chuck out obviously bad nodes
                if( 
                        mUnlikelyCandidates.matcher( unlikelyMatchString ).find() && 
                        !mOkMaybeItsACandidates.matcher( unlikelyMatchString ).find() &&
                        !tagName.equals("body")
                        )
                {
                    currentNode.removeFromTree();
                    continue;
                }
            }
            
            if(
                    tagName.equals("p") ||
                    tagName.equals("td") ||
                    tagName.equals("pre")
                    )
            {
                nodesToScore.add(currentNode);
            }
            
            // score the li directly if it has nothing interesting inside it
            if( tagName.equals("li") )
            {
                TagNode[] children = currentNode.getAllElements(true);
                boolean scoreNode = true;
                for( int j = 0; j < children.length; ++j)
                {
                    String childName = children[j].getName();
                    if( mDivToPElements.matcher( childName ).find() )
                    {
                        scoreNode = false;
                        break;
                    }
                }
                if (scoreNode)
                {
                    nodesToScore.add(currentNode);
                }
            }
            
            if( tagName.equals("div") )
            {
                TagNode[] children = currentNode.getAllElements(true);
                boolean convertNode = true;
                for( int j = 0; j < children.length; ++j)
                {
                    String childName = children[j].getName();
                    if( mDivToPElements.matcher( childName ).find() )
                    {
                        convertNode = false;
                        break;
                    }
                }
                if (convertNode)
                {
                    // System.out.println("coverted div oops" + mCleaner.getInnerHtml(currentNode));
                    currentNode.setName("p");
                    nodesToScore.add(currentNode);
                }
            }
        }
        
        
        // once we get to here, nodesToScore contains everything we want to process for content
        StringBuilder innerText = new StringBuilder();
        for( int i = 0; i < nodesToScore.size(); ++i)
        {
            TagNode node = nodesToScore.get(i);
            
            // System.out.println( node.getName() + " " + node.getText() );
            
            TagNode parentNode = node.getParent();
            
            if( parentNode == null)
            {
                continue;
            }
            TagNode grandParentNode = parentNode.getParent();
            
            if( node.getTextLength() < 25 )
            {
                continue;
            }
            
            innerText.setLength(0);
            node.getText(innerText);
            
            int contentScore = 0;
            
            /* Add a point for the paragraph itself as a base. */
            contentScore++;
            
            contentScore += getCharCount( innerText, ",");

            /* For every 100 characters in this paragraph, add another point. Up to 3 points. */
            contentScore += Math.min(innerText.length() / 100, 3);
            
            /* try link density - -3 if the whole thing is a link */
            int linkDensity = (int) (getLinkDensity(node) * -3);
            // System.out.println("link density is " + linkDensity);
            contentScore += linkDensity;
            
            // if(Globals.LOGGING) Log.d(Globals.LOG_TAG, "got score " + contentScore + "for " + node.getName() + " " + node.getAttributeByName("class") + ":" +  node.getAttributeByName("id"));
            
            /* Add the score to the parent. The grandparent gets half. */
            addScore(node, contentScore);
            addScore(parentNode, contentScore);

            if(grandParentNode != null) addScore(grandParentNode, contentScore / 2 );
        }
        
        // lets have a look at the score
        TagNode[] scoreKeys = mTagScores.keySet().toArray(new TagNode[0]);
        TagNode bestNode = null;
        int bestScore = 0;
        for( int i = 0; i < scoreKeys.length; ++i)
        {
            TagNode node = scoreKeys[i];
            int score = mTagScores.get(node);
            
            if( Globals.LOGGING )
            {
                Log.d(Globals.LOG_TAG, "score " + node.getName() + " " + node.getAttributeByName("class") + " " + node.getAttributeByName("id") + " = " + score);
            }
            
            if( bestNode == null || score > bestScore)
            {
                bestScore = score;
                bestNode = node;
            }
        }
        
        mBestScore = bestScore;
        
        if( bestNode == null )
        {
            bestNode = root.findElementByName("body", true);
        }
        
        if( bestNode == null)
        {
            return null;
        }
        
        if( Globals.LOGGING) Log.d(Globals.LOG_TAG, "The best score is " + bestScore + " " + bestNode.getText());
        
        
        // now we are going to look at the siblings of the best node to see what the output should be
        int siblingScoreThreshold = (int) Math.max(10, bestScore * 0.2 );
        
        @SuppressWarnings("rawtypes")
        List siblingNodes = bestNode.getParent().getChildren();
        
        ArrayList<TagNode> outputNodes = new ArrayList<TagNode>(); 
        
        StringBuilder content = new StringBuilder();
        for(Object o: siblingNodes)
        {
            if ( ! ( o instanceof TagNode ) )
            {
                continue;
            }
            
            TagNode sibling = (TagNode) o;
            
            String siblingName = sibling.getName();
            
            Integer siblingScore = mTagScores.get(sibling);
            
            boolean output = false;
            
            int bonusScore = 0;
            
            // Give a bonus if sibling nodes and top candidates have the example same class
            if( sibling.getAttributeByName("class") != null && sibling.getAttributeByName("class").equals(bestNode.getAttributeByName("class")) )
            {
                bonusScore += bestScore / 5;
            }
            
            if( sibling == bestNode )
            {
                output = true;
            }
            
            
            if( siblingScore != null && (siblingScore + bonusScore) >= siblingScoreThreshold )
            {
                output = true;
            }
            else if( siblingName.equals("p"))
            {
                double linkDensity = getLinkDensity(sibling);
                content.setLength(0);
                sibling.getText(content);
                
                if( content.length() > 80 && linkDensity < 0.25 )
                {
                    output = true;
                }
                else if ( content.length() < 80 && linkDensity == 0 && mSentencePattern.matcher( content ).find() )
                {
                    output = true;
                }
            }
            
            if( output )
            {
                if( siblingName.equals("div") || siblingName.equals("p") )
                {
                }
                else
                {
                    // System.out.println("forcing node type " + siblingName + " to div for output");
                    // the node is not a div or p, something trickier
                    sibling.setName("div");
                }
                
                outputNodes.add( sibling );
            }
        }
        
        TagNode article = new TagNode("div");
        
        for( int i = 0; i < outputNodes.size(); ++i )
        {
            TagNode node = outputNodes.get(i);
            node.removeFromTree();
            article.addChild(node);
        }
        
        
        return article;
    }
    
    void cleanArticle(TagNode article)
    {
        // readability.cleanStyles(articleContent);
        // readability.killBreaks(articleContent); // TODO - remove consecutive <br> <br> <br>
        
        // System.out.println("starting to clean article" + mCleaner.getInnerHtml(article));

        /* Clean out junk from the article */
        cleanConditionally(article, "form");
        clean(article, "object");
        clean(article, "h1");

        /**
         * If there is only one h2, they are probably using it
         * as a header and not a subheader, so remove it since we already have a header.
        ***/
        if(article.getElementListByName("h2", true).size() == 1)
        {
            clean(article, "h2");
        }
        clean(article, "iframe");

        cleanHeaders(article);

        /* Do these last as the previous stuff may have removed junk that will affect these */
        cleanConditionally(article, "table");
        cleanConditionally(article, "ul");
        cleanConditionally(article, "div");

        /* Remove extra paragraphs */
        TagNode[] articleParagraphs = article.getElementsByName("p", true);
        for(int i = 0; i < articleParagraphs.length; ++i)
        {
            TagNode p = articleParagraphs[i];
            int imgCount    = p.getElementListByName("img", true).size();
            int embedCount  = p.getElementListByName("embed", true).size();
            int objectCount = p.getElementListByName("object", true).size();
            
            if (imgCount == 0 && embedCount == 0 && objectCount == 0 && p.getTextLength() == 0)
            {
                p.removeFromTree();
            }
        }

        // TODO - replace <br><p> with <p> ?
        // articleContent.innerHTML = articleContent.innerHTML.replace(/<br[^>]*>\s*<p/gi, '<p');      
    }
    
    void clean(TagNode root, String tag)
    {
        // TODO - if tag is object or embed, check for youtubeage
        TagNode[] nodes = root.getElementsByName(tag, true);
        
        for( int i = 0; i < nodes.length; ++i)
        {
            nodes[i].removeFromTree();
        }
    }
    
    /**
     * remove h1 and h2 tags if they are junk
     * @param root
     */
    void cleanHeaders(TagNode root)
    {
        for (int headerIndex = 1; headerIndex < 3; headerIndex++)
        {
            TagNode[] headers = root.getElementsByName("h" + headerIndex, true);
            for (int i = 0; i < headers.length; ++i)
            {
                if (getClassWeight(headers[i]) < 0 || getLinkDensity(headers[i]) > 0.33)
                {
                    headers[i].removeFromTree();
                }
            }
        }
    }
    
    
    void cleanConditionally(TagNode root, String tag)
    {
        TagNode[] tags = root.getElementsByName(tag, true);
        
        StringBuilder nodeText = new StringBuilder();

        for( int i = 0; i < tags.length; ++i)
        {
            TagNode node = tags[i];
            
            int weight = getClassWeight(node);
            int contentScore = 0;
            
            Integer tmp = mTagScores.get(node);
            if( tmp != null )
            {
                contentScore = tmp;
            }
            
            // System.out.println("Cleaning Conditionally " + node.getName() + " (" + node.getAttributeByName("class") + ":" + node.getAttributeByName("id") + ")" + contentScore);
            
            nodeText.setLength(0);
            node.getText( nodeText );
            
            if(weight+contentScore < 0)
            {
                // System.out.println("removing because of weight " + (weight + contentScore));
                node.removeFromTree();
            }
            else if ( getCharCount(nodeText, ",") < 10)
            {
                /**
                 * If there are not very many commas, and the number of
                 * non-paragraph elements is more than paragraphs or other ominous signs, remove the element.
                **/
                int p      = node.getElementListByName("p", true).size();
                int img    = node.getElementListByName("img", true).size();
                int li     = node.getElementListByName("li", true).size()-100;
                int input  = node.getElementListByName("input", true).size();
                int embed  = node.getElementListByName("embed", true).size();
                
                // TODO - look for youtubes and keep them
                
                double linkDensity   = getLinkDensity( node );
                int contentLength = nodeText.length();
                boolean toRemove      = false;
                boolean forceKeep     = false;
                
                // System.out.println("p " + p + " img " + img + " weight " + weight + " linkDensity " + linkDensity + " embed " + embed + " content length " + contentLength + node.getText());

                if ( img > p && img > 1 ) // TODO - added this img > 1 check, see how bad this makes things
                {
                    // System.out.println("removing 1");
                    toRemove = true;
                }
                else if( li > p && ! tag.equals("ul") && ! tag.equals("ol") )
                {
                    // System.out.println("removing 2");
                    toRemove = true;
                }
                else if( input > p/3 )
                {
                    // System.out.println("removing 3");
                    toRemove = true; 
                }
                else if(contentLength < 25 && (img == 0 || img > 2) )
                {
                    // System.out.println("removing 4");
                    toRemove = true;
                }
                else if(weight < 25 && linkDensity > 0.2)
                {
                    // System.out.println("removing 5");
                    toRemove = true;
                }
                else if(weight >= 25 && linkDensity > 0.5)
                {
                    // System.out.println("removing 6");
                    toRemove = true;
                }
                else if((embed == 1 && contentLength < 75) || embed > 1)
                {
                    // System.out.println("removing 7");
                    toRemove = true;
                }
                
                // TODO - dirty hack for bbc image galleries
                if( "galMain".equals(node.getAttributeByName("class")))
                {
                    forceKeep = true;
                }
                
                if (toRemove && !forceKeep)
                {
                    // System.out.println("removing");
                    node.removeFromTree();
                }
                else
                {
                    // System.out.println("stays, hooray");
                }
            }
            else
            {
                // System.out.println("stays, heaps of commas");
            }
        }
    }
    
    private int getCharCount(StringBuilder text, String c)
    {
        int count = 0;
        
        /* Add points for any commas within this paragraph */
        int index = -1;
        while( ( index = text.indexOf(c, index+1) ) != -1 )
        {
            ++count;
        }
        
        return count;
    }
    

    public String getProcessedArticle()
    {
        TagNode html = new TagNode("html");
        TagNode head = new TagNode("head");
        TagNode title = new TagNode("title");
        title.addChild( new ContentNode(mTitle) );
        head.addChild( title );
        html.addChild( head );
        
        if( mViewport != null )
        {
            TagNode viewport = new TagNode("meta");
            viewport.setAttribute("name", "viewport");
            viewport.setAttribute("content", mViewport);
            head.addChild(viewport);
        }
        
        TagNode styleSheetNode = new TagNode("style");
        styleSheetNode.setAttribute("type", "text/css");
        styleSheetNode.addChild(new ContentNode("" +
        		"DIV { font-family: sans-serif }" +
        		"P   { font-family: sans-serif }" +
        		"H1  { text-align: center; font-family: serif }" +
        		"IMG { max-width: 100% ; height: auto } "
                )
        );
        head.addChild(styleSheetNode);
        
        if( mForcePageWidth )
        {
            TagNode viewportNode = new TagNode("meta");
            viewportNode.setAttribute("name", "viewport");
            viewportNode.setAttribute("content", "width=device-width");
            head.addChild(viewportNode);
        }
        
        if( mArticleRoot.getName().equals("body"))
        {
            mArticleRoot.setName("div");
        }
        
        TagNode body = new TagNode("body");
        
        html.addChild( body );
        
        
        TagNode content = new TagNode("div"); // TODO - set class
        TagNode header = new TagNode("h1");
        header.addChild( new ContentNode( mTitle) );
        content.addChild(header);
        
        if( mAuthor != null && mAuthor.length() > 0 )
        {
            header = new TagNode("div");
            header.addChild( new ContentNode( mAuthor ) );
            content.addChild(header);
        }
        
        if( mPubDate != null && mPubDate.length() > 0 )
        {
            header = new TagNode("div");
            header.addChild( new ContentNode( mPubDate ) );
            content.addChild(header);
        }
        
        content.addChild(new TagNode("hr"));
        content.addChild( mArticleRoot );
        body.addChild(content);
    
        html.setDocType(new DoctypeToken("html", "PUBLIC", "-//W3C//DTD HTML 4.01//EN", "http://www.w3.org/TR/html4/strict.dtd"));
        
        mArticleRoot.traverse(new TagNodeVisitor() {
            
            public boolean visit(TagNode tagNode, org.htmlcleaner.HtmlNode htmlNode)
            {
                if (htmlNode instanceof TagNode)
                {
                    TagNode tag = (TagNode) htmlNode;
                    String tagName = tag.getName();
                    
                    if( mBlacklistTags.contains(tagName) )
                    {
                        tag.removeFromTree();
                    }
                    else if (mConvertTags.containsKey(tagName))
                    {
                        tagName = mConvertTags.get(tagName);
                        tag.setName(tagName);
                    }
                    
                    Set<String> allowedAttributes = mAllowedAttributes.get( tagName );
                    
                    // convert unknown tags to div
                    if( allowedAttributes == null )
                    {
                        tagName = "div";
                        tag.setName(tagName);
                        tag.getAttributes().clear();
                    }
                    else
                    {
                        Map<String,String> attributesMap = tag.getAttributes();
                        String[] attributeNames = attributesMap.keySet().toArray(new String[0]);
                        
                        for( int i = 0; i < attributeNames.length; ++i )
                        {
                            String name = attributeNames[i];
                            if( ! allowedAttributes.contains( name ) )
                            {
                                attributesMap.remove(name);
                            }
                        }
                    }
                }
                // tells visitor to continue traversing the DOM tree
                return true;
            }
        });
        
        try
        {
            return new BrowserCompactXmlSerializer( mCleaner.getProperties() ).getAsString( html );
        }
        catch( IOException exc )
        {
            return "";
        }
    }
    
    /**
     * replace all <br><br> in the body with </p><p> for better parsing
     * TODO - see if there are any frames any maybe use those instead, readability looks at how big the frames are on the screen
     * * @param root
     */
    void preprocess(TagNode root)
    {
        TagNode body = root.findElementByName("body", true);
        
        final ArrayList<Object> contentToTransform = new ArrayList<Object>();
        final ArrayList<TagNode> brsToRemove = new ArrayList<TagNode>();
        
        if( body != null )
        {
            // traverse whole DOM and update images to absolute URLs
            body.traverse(new TagNodeVisitor()
            {
                public boolean visit(TagNode tagNode, HtmlNode htmlNode)
                {
                    if (htmlNode instanceof TagNode)
                    {
                        TagNode tag = (TagNode) htmlNode;
                        
                        // grab all child nodes and see if there are any duplicate <br><br> tags
                        Object[] children = tag.getChildren().toArray();
                        contentToTransform.clear();
                        brsToRemove.clear();
                        int brCount = 0;
                        boolean hasDoneTransform = false;
                        
                        // so we are scanning for <br>(whitespace)<br>(content)
                        for( int i = 0; i < children.length + 1; ++i )
                        {
                            Object child = null;
                            
                            if( i < children.length )
                            {
                                child = children[i];
                            }
                            
                            boolean isBr = false;
                            boolean doTransform = false;
                            
                            if( child instanceof TagNode )
                            {
                                TagNode childTag = (TagNode) child;
                                if( childTag.getName().equals("br") )
                                {
                                    isBr = true;
                                    
                                    brsToRemove.add(childTag);
                                    
                                    // we have 2 brs previously and then some content, now this br, time to transform
                                    if( brCount > 1 && contentToTransform.size() > 0 )
                                    {
                                        doTransform = true;
                                    }
                                    brCount += 1;
                                }
                            }
                            
                            // if we have found a br previously, see if the next node is junk, if so we keep scanning
                            // otherwise we reset
                            if( brCount == 1 && !isBr && child != null)
                            {
                                boolean reset = true;
                                if( child instanceof ContentNode )
                                {
                                    ContentNode childContent = (ContentNode) child;
                                    String content = childContent.getContent().toString().trim();
                                    if( content.length() == 0)
                                    {
                                        reset = false;
                                    }
                                }
                                
                                if( reset )
                                {
                                    // System.out.println("resetting on " + child + ":" + child.getClass());
                                    brCount = 0;
                                    hasDoneTransform = false;
                                    contentToTransform.clear();
                                    brsToRemove.clear();
                                }
                            }
                            
                            if( brCount > 0 && !isBr && child != null)
                            {
                                // System.out.println("Appending tranformation candidate " + contentToTransform.size());
                                contentToTransform.add(child);
                            }
                            
                            if( brCount > 1 && child == null && hasDoneTransform)
                            {
                                doTransform = true;
                            }
                            
                            if( doTransform )
                            {
                                // System.out.println("PerformingTransform");
                                hasDoneTransform = true;
                                
                                TagNode newParagraph = null;
                                
                                if( brsToRemove.size() > 0 )
                                {
                                    newParagraph = brsToRemove.get(0);
                                }
                                else // this is the last paragraph, make a new one
                                {
                                    newParagraph = new TagNode("p");
                                    tag.addChild(newParagraph);
                                    hasDoneTransform = false;
                                }
                                
                                newParagraph.setName("p"); // turn silly old <br> into a shiny <p> to stick the new content under
                                
                                for(int j = 0; j < contentToTransform.size(); ++j)
                                {
                                    Object transformChild = contentToTransform.get(j);
                                    
                                    if( transformChild instanceof TagNode )
                                    {
                                        ((TagNode) transformChild).removeFromTree();
                                    }
                                    
                                    tag.removeChild(transformChild);
                                    
                                    newParagraph.addChild( transformChild );
                                }
                                
                                for( int j = 1; j < brsToRemove.size() - 1; ++j )
                                {
                                    brsToRemove.get(j).removeFromTree();
                                }
                                
                                // System.out.println("new content is " + mCleaner.getInnerHtml(newParagraph));
                                
                                contentToTransform.clear();
                                TagNode savedBr = brsToRemove.get( brsToRemove.size() - 1);
                                brsToRemove.clear();
                                brsToRemove.add(savedBr);
                                brCount = 2; // initilise in state as if we have previously seen some brs so consective paragraphs get transformed
                            }
                        }
                        
                        // System.out.println("finished processing children");
                        
                        contentToTransform.clear();
                        brsToRemove.clear();
                    }
                    // tells visitor to continue traversing the DOM tree
                    return true;
                }
            });
        }
    }
}
