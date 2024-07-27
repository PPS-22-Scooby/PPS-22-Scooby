package org.unibo.scooby
package utility.rule


object Document:
  
  final case class Link(text: String, href: String)
  final case class Tag(id: String, text: String)
  final case class Document(links: Seq[Link], tags: Seq[Tag]):
    
    def selectTag(id: String): Document =
      Document(links, tags.filterNot:
        case Tag(i, _) => id != i
      )
    
    def selectLinkByText(text: String): Document =
      Document(links.filterNot:
        case Link(t, _) => text != t,
        tags
      )
  
    def filterLinksByLink(href: String): Document =
      Document(links.filterNot:
        case Link(_, h) => href != h,
        tags
      )
  
    def notIn(seq: Seq[Link]): Document =
      Document(links.filterNot(seq.contains(_)), tags)
      
    def containLink(link: Link): Boolean = links.contains(link)

